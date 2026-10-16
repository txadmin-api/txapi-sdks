package cloud.txapi.sdk.webhooks;

import cloud.txapi.sdk.errors.WebhookVerificationException;
import cloud.txapi.sdk.internal.Json;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhooksTest {
    private static JsonNode vectors;
    private static String secret;
    private static String body;
    private static Instant now;
    private static Duration tolerance;

    @BeforeAll
    static void load() throws Exception {
        vectors = Json.MAPPER.readTree(Files.readAllBytes(Path.of("..", "spec", "webhook-vectors.json")));
        secret = vectors.path("secret").asText();
        body = vectors.path("body").asText();
        now = Instant.ofEpochSecond(vectors.path("nowSeconds").asLong());
        tolerance = Duration.ofSeconds(vectors.path("toleranceSeconds").asLong());
    }

    @Test
    void acceptsTheValidVector() {
        WebhookEvent e = Webhooks.verify(body.getBytes(StandardCharsets.UTF_8), vectors.at("/valid/header").asText(),
                secret, tolerance, now);
        assertThat(e.id()).isEqualTo("5f0c8a52-6c3c-4c1e-9d0e-2b6f2d1f3a10");
        assertThat(e.event()).isEqualTo(WebhookEvents.PLAYER_BANNED);
        assertThat(e.is("player.banned")).isTrue();
        assertThat(e.createdAt()).isEqualTo(Instant.parse("2025-10-09T08:53:20Z"));
        assertThat(e.serverId()).isEqualTo(1L);
        assertThat(e.server()).hasValue(1L);
        assertThat(e.data().at("/request/durationHours").asInt()).isEqualTo(24);
    }

    @Test
    void acceptsTheStringBodyAndAClock() {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        assertThat(Webhooks.verify(body.getBytes(StandardCharsets.UTF_8), vectors.at("/valid/header").asText(), secret,
                tolerance, clock).event()).isEqualTo("player.banned");
        assertThat(Webhooks.verify(body, vectors.at("/valid/header").asText(), secret, tolerance, now).serverId())
                .isEqualTo(1L);
    }

    @Test
    void acceptsAnyOfSeveralSignatures() {
        WebhookEvent e = Webhooks.verify(body, vectors.at("/validWithRotatedSecret/header").asText(), secret, tolerance, now);
        assertThat(e.event()).isEqualTo("player.banned");
    }

    static List<JsonNode> invalidCases() throws Exception {
        load();
        List<JsonNode> out = new ArrayList<>();
        vectors.path("invalid").forEach(out::add);
        return out;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void rejectsInvalidVectors(JsonNode c) {
        String b = c.has("body") ? c.path("body").asText() : body;
        assertThatThrownBy(() -> Webhooks.verify(b.getBytes(StandardCharsets.UTF_8), c.path("header").asText(), secret,
                tolerance, now)).as(c.path("name").asText()).isInstanceOf(WebhookVerificationException.class);
    }

    @Test
    void reasonsAreSpecific() {
        assertThatThrownBy(() -> Webhooks.verify(body, null, secret, tolerance, now))
                .hasMessageContaining("missing");
        assertThatThrownBy(() -> Webhooks.verify(body, "t=1759999000,v1=c7ae0652ac7310f7d7310a042486eba57b83e7f37a2ed80601c20553d9394757",
                secret, tolerance, now)).hasMessageContaining("old");
        assertThatThrownBy(() -> Webhooks.verify(body, vectors.at("/valid/header").asText(), "whsec_wrong", tolerance, now))
                .hasMessageContaining("does not match");
    }

    @Test
    void reserialisedJsonFailsBecauseTheRawBodyMatters() throws Exception {
        String reserialised = Json.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(Json.MAPPER.readTree(body));
        assertThatThrownBy(() -> Webhooks.verify(reserialised, vectors.at("/valid/header").asText(), secret, tolerance, now))
                .isInstanceOf(WebhookVerificationException.class);
    }

    @Test
    void signRoundTrips() {
        String header = Webhooks.sign(body, secret, now.getEpochSecond() - 60);
        assertThat(header).isEqualTo(vectors.at("/valid/header").asText());
        long t = Instant.now().getEpochSecond();
        String fresh = Webhooks.sign("{\"id\":\"x\",\"event\":\"ping\",\"data\":{}}", "whsec_abc", t);
        WebhookEvent e = Webhooks.verify("{\"id\":\"x\",\"event\":\"ping\",\"data\":{}}", fresh, "whsec_abc");
        assertThat(e.event()).isEqualTo(WebhookEvents.PING);
        assertThat(e.serverId()).isNull();
        assertThat(e.createdAt()).isNull();
    }

    @Test
    void customToleranceIsApplied() {
        String header = vectors.at("/valid/header").asText();
        assertThatThrownBy(() -> Webhooks.verify(body.getBytes(StandardCharsets.UTF_8), header, secret,
                Duration.ofSeconds(30), now)).hasMessageContaining("old");
    }

    @Test
    void signedNonEventBodyIsRejected() {
        long t = Instant.now().getEpochSecond();
        assertThatThrownBy(() -> Webhooks.verify("[1,2]", Webhooks.sign("[1,2]", "s", t), "s"))
                .isInstanceOf(WebhookVerificationException.class).hasMessageContaining("not a webhook event");
        assertThatThrownBy(() -> Webhooks.verify("not json", Webhooks.sign("not json", "s", t), "s"))
                .isInstanceOf(WebhookVerificationException.class).hasMessageContaining("not JSON");
    }

    @Test
    void blankSecretIsAProgrammingError() {
        assertThatThrownBy(() -> Webhooks.verify(body, vectors.at("/valid/header").asText(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eventConstantsMatchTheSpec() throws Exception {
        JsonNode endpoints = Json.MAPPER.readTree(Files.readAllBytes(Path.of("..", "spec", "endpoints.json")));
        Set<String> spec = new TreeSet<>();
        endpoints.path("webhookEvents").forEach(n -> spec.add(n.asText()));
        assertThat(new TreeSet<>(WebhookEvents.ALL)).isEqualTo(spec);
        assertThat(Webhooks.SIGNATURE_HEADER).isEqualTo("X-Txapi-Signature");
        assertThat(Webhooks.EVENT_HEADER).isEqualTo("X-Txapi-Event");
        assertThat(Webhooks.DELIVERY_HEADER).isEqualTo("X-Txapi-Delivery");
    }
}
