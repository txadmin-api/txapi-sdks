package cloud.txapi.sdk;

import cloud.txapi.sdk.errors.ApiTimeoutException;
import cloud.txapi.sdk.errors.ValidationException;
import cloud.txapi.sdk.params.BanRequest;
import cloud.txapi.sdk.params.DeathQuery;
import cloud.txapi.sdk.params.EventListQuery;
import cloud.txapi.sdk.params.PlayerFilter;
import cloud.txapi.sdk.params.PlayerSearchParams;
import cloud.txapi.sdk.params.PlayerSort;
import cloud.txapi.sdk.params.SortOrder;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestBuildingTest {
    private MockApi mock;

    @BeforeEach
    void setUp() {
        mock = new MockApi();
    }

    @AfterEach
    void tearDown() {
        mock.close();
    }

    @Test
    void sendsAuthUserAgentAndAccept() {
        try (TxApi c = mock.client(b -> b.userAgent("my-bot/2.0"))) {
            c.server().status();
        }
        MockApi.Req r = mock.last();
        assertThat(r.method()).isEqualTo("GET");
        assertThat(r.rawPath()).isEqualTo("/api/v1/server/status");
        assertThat(r.header("Authorization")).isEqualTo("Bearer " + MockApi.KEY);
        assertThat(r.header("User-Agent")).isEqualTo("txapi-java/" + TxApi.VERSION + " my-bot/2.0");
        assertThat(r.header("Accept")).isEqualTo("application/json");
        assertThat(r.header("Content-Type")).isNull();
    }

    @Test
    void pathParametersArePercentEncoded() {
        try (TxApi c = mock.client()) {
            c.admins().get("name with space");
            assertThat(mock.last().rawPath()).isEqualTo("/api/v1/txadmin/admins/name%20with%20space");
            c.whitelist().approvals().remove("license:abc");
            assertThat(mock.last().rawPath()).isEqualTo("/api/v1/whitelist/approvals/license%3Aabc");
            assertThat(mock.last().method()).isEqualTo("DELETE");
            c.history().action("a/b?c");
            assertThat(mock.last().rawPath()).isEqualTo("/api/v1/history/action/a%2Fb%3Fc");
        }
    }

    @Test
    void queryParametersAreEncoded() {
        try (TxApi c = mock.client()) {
            c.players().info("license:abc def");
        }
        assertThat(mock.last().query()).containsExactly(Map.entry("identifier", "license:abc def"));
        assertThat(mock.last().rawQuery()).doesNotContain(" ").doesNotContain("+");
    }

    @Test
    void serverIdComesFromClientViewOrCall() {
        try (TxApi c = mock.client(b -> b.serverId(5L))) {
            c.server().status();
            assertThat(mock.last().query()).containsEntry("serverId", "5");

            c.forServer(9).server().status();
            assertThat(mock.last().query()).containsEntry("serverId", "9");

            c.forServer(9).server().status(RequestOptions.serverId(11));
            assertThat(mock.last().query()).containsEntry("serverId", "11");

            c.account().me();
            assertThat(mock.last().rawPath()).isEqualTo("/api/v1/me");
            assertThat(mock.last().query()).doesNotContainKey("serverId");

            assertThat(c.forServer(9).serverId()).hasValue(9);
            assertThat(c.serverId()).hasValue(5);
        }
        try (TxApi c = mock.client()) {
            c.server().status();
            assertThat(mock.last().rawQuery()).isNull();
        }
    }

    @Test
    void bodyOmitsNullsAndKeepsTypes() {
        try (TxApi c = mock.client()) {
            c.players().ban(BanRequest.of("license:abc", "cheating"));
            MockApi.Req r = mock.last();
            assertThat(r.method()).isEqualTo("POST");
            assertThat(r.header("Content-Type")).startsWith("application/json");
            JsonNode body = r.json();
            assertThat(body.size()).isEqualTo(2);
            assertThat(body.path("identifier").asText()).isEqualTo("license:abc");
            assertThat(body.path("reason").asText()).isEqualTo("cheating");

            c.players().ban(BanRequest.of("license:abc", "cheating").withDurationHours(72).withDiscordBan(true));
            body = mock.last().json();
            assertThat(body.path("durationHours").isInt()).isTrue();
            assertThat(body.path("durationHours").asInt()).isEqualTo(72);
            assertThat(body.path("discordBan").isBoolean()).isTrue();

            c.players().setWhitelisted("license:abc", false);
            body = mock.last().json();
            assertThat(body.path("whitelisted").isBoolean()).isTrue();
            assertThat(body.path("whitelisted").asBoolean()).isFalse();

            c.server().kickAll();
            assertThat(mock.last().json().size()).isZero(); // optional reason omitted → {}
        }
    }

    @Test
    void queryParamsOmitNullsAndFormatValues() {
        try (TxApi c = mock.client()) {
            c.players().search(PlayerSearchParams.create().query("bob").sortBy(PlayerSort.JOINED)
                    .order(SortOrder.ASC).filters(PlayerFilter.ONLINE, PlayerFilter.ADMIN));
            assertThat(mock.last().query()).containsExactly(
                    Map.entry("query", "bob"), Map.entry("sortBy", "tsJoined"), Map.entry("order", "asc"),
                    Map.entry("filters", "isOnline,isAdmin"));

            c.events().deaths(DeathQuery.create().pvp(true).since(Duration.ofHours(24)).limit(50));
            assertThat(mock.last().query()).containsEntry("pvp", "true").containsEntry("since", "1d")
                    .containsEntry("limit", "50");

            c.events().deaths(DeathQuery.create().since(Instant.parse("2026-10-01T12:00:00Z")).until(Duration.ofMinutes(90)));
            assertThat(mock.last().query()).containsEntry("since", "2026-10-01T12:00:00Z").containsEntry("until", "90m");

            c.events().list(EventListQuery.create().type("death", " ban ", "death"));
            assertThat(mock.last().query()).containsEntry("type", "death,ban");
            assertThat(EventListQuery.create().type("death").type(new String[0]).type()).isNull();
        }
    }

    @Test
    void invalidInputFailsWithoutNetwork() {
        try (TxApi c = mock.client()) {
            assertThatThrownBy(() -> c.players().ban(BanRequest.of("license:abc", "  ")))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> assertThat(((ValidationException) e).field()).isEqualTo("reason"));
            assertThatThrownBy(() -> c.players().ban(BanRequest.of("license:abc", "x".repeat(513))))
                    .isInstanceOf(ValidationException.class).hasMessageContaining("512");
            assertThatThrownBy(() -> c.console().command("x".repeat(1025)))
                    .isInstanceOf(ValidationException.class).hasMessageContaining("1024");
            assertThatThrownBy(() -> c.admins().get(""))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> c.admins().add("mod", List.of()))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> assertThat(((ValidationException) e).field()).isEqualTo("permissions"));
            assertThatThrownBy(() -> c.players().info(null)).isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> PlayerSearchParams.create().sortBy("bogus"))
                    .isInstanceOf(IllegalArgumentException.class);

            var future = c.async().players().ban(BanRequest.of("license:abc", ""));
            assertThat(future).isCompletedExceptionally();
            assertThatThrownBy(future::join).isInstanceOf(CompletionException.class)
                    .hasCauseInstanceOf(ValidationException.class);
        }
        assertThat(mock.requests).isEmpty();
    }

    @Test
    void timeoutRaisesApiTimeoutException() {
        mock.handler(r -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return MockApi.Resp.ok("{}");
        });
        try (TxApi c = mock.client(b -> b.timeout(Duration.ofMillis(300)).maxRetries(0))) {
            assertThatThrownBy(() -> c.server().status()).isInstanceOf(ApiTimeoutException.class);
            // per-call timeout wins over the client's
            assertThatThrownBy(() -> c.server().status(RequestOptions.builder().timeout(Duration.ofMillis(200)).build()))
                    .isInstanceOf(ApiTimeoutException.class);
        }
    }

    @Test
    void builderNormalisesAndValidates() {
        assertThat(TxApi.Builder.normaliseBaseUrl("https://txapi.cloud/api/v1/")).isEqualTo("https://txapi.cloud");
        assertThat(TxApi.Builder.normaliseBaseUrl("http://localhost:8080//")).isEqualTo("http://localhost:8080");
        assertThatThrownBy(() -> TxApi.Builder.normaliseBaseUrl("ftp://x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TxApi.Builder.normaliseBaseUrl("txapi.cloud")).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TxApi.builder().env(k -> null).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("TXAPI_KEY");

        Map<String, String> env = Map.of("TXAPI_KEY", "txp_env", "TXAPI_BASE_URL", mock.baseUrl() + "/api/v1",
                "TXAPI_SERVER_ID", "3");
        try (TxApi c = TxApi.builder().env(env::get).build()) {
            assertThat(c.baseUrl()).isEqualTo(mock.baseUrl());
            assertThat(c.serverId()).hasValue(3);
            c.server().status();
            assertThat(mock.last().header("Authorization")).isEqualTo("Bearer txp_env");
            assertThat(mock.last().query()).containsEntry("serverId", "3");
        }
        assertThatThrownBy(() -> TxApi.builder().apiKey("k").env(k -> null).timeout(Duration.ZERO).build())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TxApi.builder().apiKey("k").env(k -> null).maxRetries(-1).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringNeverContainsTheKey() {
        try (TxApi c = mock.client(b -> b.serverId(1L))) {
            assertThat(c.toString()).doesNotContain(MockApi.KEY).contains("***");
            assertThat(c.async().toString()).doesNotContain(MockApi.KEY);
            assertThat(c.forServer(2).toString()).doesNotContain(MockApi.KEY);
        }
    }

    @Test
    void closedClientRejectsCalls() {
        TxApi c = mock.client();
        c.close();
        c.close(); // idempotent
        assertThatThrownBy(() -> c.server().status()).isInstanceOf(IllegalStateException.class);
        assertThat(c.async().server().status()).isCompletedExceptionally();
    }

    @Test
    void viewsDoNotCloseTheClient() {
        try (TxApi c = mock.client()) {
            c.forServer(1).close();
            c.server().status();
            assertThat(mock.requests).hasSize(1);
        }
    }
}
