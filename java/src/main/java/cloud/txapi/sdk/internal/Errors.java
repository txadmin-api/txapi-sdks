package cloud.txapi.sdk.internal;

import cloud.txapi.sdk.RateLimit;
import cloud.txapi.sdk.errors.ApiException;
import cloud.txapi.sdk.errors.AuthenticationException;
import cloud.txapi.sdk.errors.BadRequestException;
import cloud.txapi.sdk.errors.ConflictException;
import cloud.txapi.sdk.errors.NotFoundException;
import cloud.txapi.sdk.errors.PermissionDeniedException;
import cloud.txapi.sdk.errors.RateLimitException;
import cloud.txapi.sdk.errors.ServerException;
import cloud.txapi.sdk.errors.TxAdminRejectedException;
import cloud.txapi.sdk.errors.TxAdminUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Maps an HTTP error response to the SDK exception hierarchy (SPEC §4). */
public final class Errors {
    private Errors() {
    }

    public static ApiException fromResponse(int status, String rawBody, Map<String, List<String>> headers,
                                            RateLimit rateLimit) {
        JsonNode body = Json.tryParse(rawBody);
        String message = message(status, body, rawBody);
        return switch (status) {
            case 400, 405, 415 -> new BadRequestException(status, message, body, rawBody, headers, rateLimit);
            case 401 -> new AuthenticationException(status, message, body, rawBody, headers, rateLimit);
            case 403 -> new PermissionDeniedException(status, message, body, rawBody, headers, rateLimit);
            case 404 -> new NotFoundException(status, message, body, rawBody, headers, rateLimit);
            case 409 -> new ConflictException(status, message, body, rawBody, headers, rateLimit);
            case 422 -> new TxAdminRejectedException(status, message, body, rawBody, headers, rateLimit);
            case 429 -> {
                String ra = first(headers, "retry-after");
                Duration retryAfter = parseRetryAfter(ra);
                yield new RateLimitException(status, message, body, rawBody, headers, rateLimit,
                        retryAfter, ra == null);
            }
            case 502 -> new TxAdminUnavailableException(status, message, body, rawBody, headers, rateLimit);
            default -> status >= 500
                    ? new ServerException(status, message, body, rawBody, headers, rateLimit)
                    : new ApiException(status, message, body, rawBody, headers, rateLimit);
        };
    }

    /** The {@code error} text, else {@code message}, else the body text, else the reason phrase. */
    static String message(int status, JsonNode body, String rawBody) {
        if (body != null && body.isObject()) {
            for (String field : new String[]{"error", "message"}) {
                JsonNode n = body.get(field);
                if (n != null && !n.isNull()) {
                    String text = n.isTextual() ? n.asText() : n.toString();
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }
        if (rawBody != null && !rawBody.isBlank()) {
            String t = rawBody.strip();
            return t.length() > 500 ? t.substring(0, 500) + "…" : t;
        }
        return status + " " + reasonPhrase(status);
    }

    public static String first(Map<String, List<String>> headers, String name) {
        List<String> v = headers.get(name.toLowerCase(Locale.ROOT));
        return v == null || v.isEmpty() ? null : v.get(0);
    }

    /** {@code Retry-After} as seconds or HTTP date; {@code null} when absent or unreadable. */
    public static Duration parseRetryAfter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        try {
            double seconds = Double.parseDouble(v);
            return seconds < 0 ? Duration.ZERO : Duration.ofMillis((long) Math.ceil(seconds * 1000));
        } catch (NumberFormatException ignored) {
            // maybe an HTTP date
        }
        try {
            Instant at = ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            Duration d = Duration.between(Instant.now(), at);
            return d.isNegative() ? Duration.ZERO : d;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static String reasonPhrase(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 415 -> "Unsupported Media Type";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "HTTP error";
        };
    }
}
