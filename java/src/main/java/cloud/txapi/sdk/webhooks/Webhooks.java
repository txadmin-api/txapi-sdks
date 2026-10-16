package cloud.txapi.sdk.webhooks;

import cloud.txapi.sdk.errors.WebhookVerificationException;
import cloud.txapi.sdk.internal.Json;
import com.fasterxml.jackson.databind.JsonNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Verifies webhook deliveries sent by txApi.
 *
 * <p>Every delivery carries {@code X-Txapi-Signature: t=<unix seconds>,v1=<hex>}, where the
 * signature is {@code HMAC-SHA256(secret, "<t>.<raw body>")}. While a secret is being rotated the
 * header can carry several {@code v1} entries; the delivery is valid if any of them matches.
 *
 * <p><b>Pass the raw request body exactly as received.</b> Parsing the JSON and serialising it
 * again (a Spring {@code @RequestBody Map}, a Jackson round-trip …) changes whitespace or key
 * order and the signature no longer matches. In Spring take {@code @RequestBody byte[] body}.
 *
 * <pre>{@code
 * WebhookEvent event = Webhooks.verify(rawBody, request.getHeader(Webhooks.SIGNATURE_HEADER), secret);
 * if (event.is(WebhookEvents.PLAYER_BANNED)) { ... }
 * }</pre>
 *
 * <p>Deliveries are retried after failures and may arrive more than once; deduplicate on
 * {@link WebhookEvent#id()}.
 */
public final class Webhooks {
    /** Header with timestamp and signature(s). */
    public static final String SIGNATURE_HEADER = "X-Txapi-Signature";
    /** Header with the event name (also in the body). */
    public static final String EVENT_HEADER = "X-Txapi-Event";
    /** Header with the delivery id (also in the body as {@code id}). */
    public static final String DELIVERY_HEADER = "X-Txapi-Delivery";
    /** Accepted clock difference between signing and verifying (5 minutes). */
    public static final Duration DEFAULT_TOLERANCE = Duration.ofSeconds(300);

    private static final HexFormat HEX = HexFormat.of();

    private Webhooks() {
    }

    /**
     * Verifies a delivery with the default tolerance of 5 minutes.
     *
     * @param rawBody         the request body bytes, exactly as received
     * @param signatureHeader value of {@value #SIGNATURE_HEADER} (may be {@code null})
     * @param secret          the endpoint's signing secret ({@code whsec_…})
     * @return the verified event
     * @throws WebhookVerificationException if the delivery is not authentic, too old or not an event
     * @throws IllegalArgumentException     if {@code secret} is blank
     */
    public static WebhookEvent verify(byte[] rawBody, String signatureHeader, String secret) {
        return verify(rawBody, signatureHeader, secret, DEFAULT_TOLERANCE, Instant.now());
    }

    /**
     * Verifies a delivery whose body you already hold as a string. Only use this when the string
     * is the unmodified body (decoded as UTF-8); prefer {@link #verify(byte[], String, String)}.
     *
     * @param rawBody         the request body, exactly as received
     * @param signatureHeader value of {@value #SIGNATURE_HEADER}
     * @param secret          the signing secret
     * @return the verified event
     * @throws WebhookVerificationException if the delivery cannot be verified
     */
    public static WebhookEvent verify(String rawBody, String signatureHeader, String secret) {
        return verify(bytes(rawBody), signatureHeader, secret);
    }

    /**
     * Verifies a delivery with a custom tolerance.
     *
     * @param rawBody         the request body bytes
     * @param signatureHeader value of {@value #SIGNATURE_HEADER}
     * @param secret          the signing secret
     * @param tolerance       accepted difference between the signature timestamp and now
     * @return the verified event
     * @throws WebhookVerificationException if the delivery cannot be verified
     */
    public static WebhookEvent verify(byte[] rawBody, String signatureHeader, String secret, Duration tolerance) {
        return verify(rawBody, signatureHeader, secret, tolerance, Instant.now());
    }

    /**
     * Verifies a delivery against a given clock.
     *
     * @param rawBody         the request body bytes
     * @param signatureHeader value of {@value #SIGNATURE_HEADER}
     * @param secret          the signing secret
     * @param tolerance       accepted difference between the signature timestamp and now
     * @param clock           clock providing "now"
     * @return the verified event
     * @throws WebhookVerificationException if the delivery cannot be verified
     */
    public static WebhookEvent verify(byte[] rawBody, String signatureHeader, String secret,
                                      Duration tolerance, Clock clock) {
        return verify(rawBody, signatureHeader, secret, tolerance, Objects.requireNonNull(clock, "clock").instant());
    }

    /**
     * Verifies a delivery as of a given point in time (for tests and replays).
     *
     * @param rawBody         the request body, exactly as received
     * @param signatureHeader value of {@value #SIGNATURE_HEADER}
     * @param secret          the signing secret
     * @param tolerance       accepted difference between the signature timestamp and {@code now}
     * @param now             the current time
     * @return the verified event
     * @throws WebhookVerificationException if the delivery cannot be verified
     */
    public static WebhookEvent verify(String rawBody, String signatureHeader, String secret,
                                      Duration tolerance, Instant now) {
        return verify(bytes(rawBody), signatureHeader, secret, tolerance, now);
    }

    /**
     * Verifies a delivery as of a given point in time (for tests and replays).
     *
     * @param rawBody         the request body bytes, exactly as received
     * @param signatureHeader value of {@value #SIGNATURE_HEADER}
     * @param secret          the signing secret
     * @param tolerance       accepted difference between the signature timestamp and {@code now}
     * @param now             the current time
     * @return the verified event
     * @throws WebhookVerificationException if the delivery cannot be verified
     * @throws IllegalArgumentException     if {@code secret} is blank or {@code tolerance} negative
     */
    public static WebhookEvent verify(byte[] rawBody, String signatureHeader, String secret,
                                      Duration tolerance, Instant now) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("txApi webhooks: the signing secret is empty");
        }
        Objects.requireNonNull(tolerance, "tolerance");
        Objects.requireNonNull(now, "now");
        if (tolerance.isNegative()) {
            throw new IllegalArgumentException("txApi webhooks: tolerance must not be negative");
        }
        byte[] body = rawBody == null ? new byte[0] : rawBody;
        Parsed header = parse(signatureHeader);

        byte[] expected = hmac(secret, header.timestamp, body);
        boolean match = false;
        for (byte[] candidate : header.signatures) {
            // isEqual is constant-time for equal lengths; keep looping to avoid an early exit
            match |= MessageDigest.isEqual(expected, candidate);
        }
        if (!match) {
            throw new WebhookVerificationException("txApi webhook: signature does not match"
                    + " (wrong secret, or the body was modified — verify the raw bytes)");
        }
        long age = now.getEpochSecond() - header.timestamp;
        if (Math.abs(age) > tolerance.getSeconds()) {
            throw new WebhookVerificationException(age > 0
                    ? "txApi webhook: timestamp is " + age + " s old (tolerance " + tolerance.getSeconds() + " s)"
                    : "txApi webhook: timestamp is " + (-age) + " s in the future (tolerance "
                    + tolerance.getSeconds() + " s)");
        }
        return parseEvent(body);
    }

    /**
     * Computes a signature header the way txApi does — for tests of your webhook receiver.
     *
     * @param rawBody          the body
     * @param secret           the signing secret
     * @param timestampSeconds unix seconds to sign
     * @return a value for {@value #SIGNATURE_HEADER}
     */
    public static String sign(byte[] rawBody, String secret, long timestampSeconds) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("txApi webhooks: the signing secret is empty");
        }
        return "t=" + timestampSeconds + ",v1="
                + HEX.formatHex(hmac(secret, timestampSeconds, rawBody == null ? new byte[0] : rawBody));
    }

    /**
     * String variant of {@link #sign(byte[], String, long)} (body encoded as UTF-8).
     *
     * @param rawBody          the body
     * @param secret           the signing secret
     * @param timestampSeconds unix seconds to sign
     * @return a value for {@value #SIGNATURE_HEADER}
     */
    public static String sign(String rawBody, String secret, long timestampSeconds) {
        return sign(bytes(rawBody), secret, timestampSeconds);
    }

    // ── internals ───────────────────────────────────────────────────────────

    private record Parsed(long timestamp, List<byte[]> signatures) {
    }

    private static Parsed parse(String header) {
        if (header == null || header.isBlank()) {
            throw new WebhookVerificationException("txApi webhook: missing " + SIGNATURE_HEADER + " header");
        }
        Long timestamp = null;
        List<byte[]> signatures = new ArrayList<>();
        boolean sawV1 = false;
        for (String part : header.split(",")) {
            String p = part.strip();
            int eq = p.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = p.substring(0, eq).strip();
            String value = p.substring(eq + 1).strip();
            if (key.equals("t")) {
                if (timestamp != null) {
                    throw new WebhookVerificationException("txApi webhook: malformed signature header (two timestamps)");
                }
                if (value.isEmpty() || value.length() > 12 || !value.chars().allMatch(Character::isDigit)) {
                    throw new WebhookVerificationException("txApi webhook: malformed timestamp in signature header");
                }
                timestamp = Long.parseLong(value);
            } else if (key.equals("v1")) {
                sawV1 = true;
                if (value.length() == 64) {
                    try {
                        signatures.add(HEX.parseHex(value.toLowerCase(java.util.Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {
                        // not hex — cannot match, skip
                    }
                }
            }
            // other schemes (future v2 …) are ignored
        }
        if (timestamp == null) {
            throw new WebhookVerificationException("txApi webhook: signature header has no timestamp (t=…)");
        }
        if (signatures.isEmpty()) {
            throw new WebhookVerificationException(sawV1
                    ? "txApi webhook: signature does not match (malformed v1 value)"
                    : "txApi webhook: signature header has no v1 signature");
        }
        return new Parsed(timestamp, signatures);
    }

    private static byte[] hmac(String secret, long timestamp, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            return mac.doFinal(body);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private static WebhookEvent parseEvent(byte[] body) {
        JsonNode root;
        try {
            root = Json.MAPPER.readTree(body);
        } catch (IOException e) {
            throw new WebhookVerificationException("txApi webhook: body is not JSON", e);
        }
        if (root == null || !root.isObject() || !root.path("event").isTextual()) {
            throw new WebhookVerificationException("txApi webhook: body is not a webhook event");
        }
        JsonNode sid = root.path("serverId");
        Long serverId = null; // not a ternary: mixing long and Long there unboxes null
        if (sid.isIntegralNumber()) {
            serverId = sid.asLong();
        } else if (sid.isTextual() && sid.asText().matches("\\d{1,18}")) {
            serverId = Long.valueOf(sid.asText());
        }
        JsonNode created = root.path("createdAt");
        Instant createdAt = created.isNumber() ? Json.epoch(created.asLong())
                : created.isTextual() ? Json.parseInstant(created.asText()) : null;
        JsonNode data = root.get("data");
        if (data == null || data.isNull() || data.isMissingNode()) {
            data = Json.MAPPER.createObjectNode();
        }
        JsonNode id = root.get("id");
        return new WebhookEvent(id == null || id.isNull() ? null : id.asText(), root.get("event").asText(),
                createdAt, serverId, data);
    }

    private static byte[] bytes(String s) {
        return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
    }
}
