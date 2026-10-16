package cloud.txapi.sdk;

import cloud.txapi.sdk.errors.ApiConnectionException;
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
import cloud.txapi.sdk.errors.TxApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class ErrorMappingTest {
    private MockApi mock;
    private TxApi client;

    @BeforeEach
    void setUp() {
        mock = new MockApi();
        client = mock.client(b -> b.maxRetries(0));
    }

    @AfterEach
    void tearDown() {
        client.close();
        mock.close();
    }

    static Stream<Arguments> statuses() {
        return Stream.of(
                Arguments.of(400, BadRequestException.class),
                Arguments.of(401, AuthenticationException.class),
                Arguments.of(403, PermissionDeniedException.class),
                Arguments.of(404, NotFoundException.class),
                Arguments.of(405, BadRequestException.class),
                Arguments.of(409, ConflictException.class),
                Arguments.of(415, BadRequestException.class),
                Arguments.of(422, TxAdminRejectedException.class),
                Arguments.of(429, RateLimitException.class),
                Arguments.of(500, ServerException.class),
                Arguments.of(502, TxAdminUnavailableException.class),
                Arguments.of(503, ServerException.class),
                Arguments.of(418, ApiException.class));
    }

    @ParameterizedTest
    @MethodSource("statuses")
    void mapsStatusToException(int status, Class<? extends ApiException> type) {
        mock.enqueue(MockApi.Resp.json(status, "{\"success\":false,\"error\":\"boom " + status + "\"}")
                .header("X-RateLimit-Limit", "60").header("X-RateLimit-Remaining", "0").header("X-RateLimit-Reset", "12"));
        Throwable t = catchThrowable(() -> client.server().status());
        assertThat(t).isExactlyInstanceOf(type);
        ApiException e = (ApiException) t;
        assertThat(e.status()).isEqualTo(status);
        assertThat(e.getMessage()).contains("boom " + status);
        assertThat(e.body().path("success").asBoolean(true)).isFalse();
        assertThat(e.rateLimit()).hasValue(new RateLimit(60L, 0L, 12L));
        assertThat(e).isInstanceOf(TxApiException.class).isInstanceOf(RuntimeException.class);
    }

    @Test
    void asyncFailsWithTheSameException() {
        mock.enqueue(MockApi.Resp.json(404, "{\"success\":false,\"error\":\"Player not found\"}"));
        assertThatThrownBy(() -> client.async().players().info("license:x").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(NotFoundException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void rateLimitWithRetryAfter() {
        mock.enqueue(MockApi.Resp.json(429, "{\"success\":false,\"error\":\"Rate limit exceeded\"}").header("Retry-After", "17"));
        RateLimitException e = (RateLimitException) catchThrowable(() -> client.server().status());
        assertThat(e.retryAfter()).hasValue(Duration.ofSeconds(17));
        assertThat(e.isQuotaExhausted()).isFalse();
    }

    @Test
    void rateLimitWithoutRetryAfterIsQuota() {
        mock.enqueue(MockApi.Resp.json(429, "{\"success\":false,\"error\":\"Monthly request quota exceeded\"}"));
        RateLimitException e = (RateLimitException) catchThrowable(() -> client.server().status());
        assertThat(e.retryAfter()).isEmpty();
        assertThat(e.isQuotaExhausted()).isTrue();
    }

    @Test
    void nonJsonBodyKeepsRawText() {
        mock.enqueue(new MockApi.Resp(502, "<html>Bad Gateway</html>", java.util.Map.of("Content-Type", "text/html")));
        ApiException e = (ApiException) catchThrowable(() -> client.server().status());
        assertThat(e).isInstanceOf(TxAdminUnavailableException.class);
        assertThat(e.rawBody()).contains("Bad Gateway");
        assertThat(e.body()).isNull();
    }

    @Test
    void messageFallsBackToMessageField() {
        mock.enqueue(MockApi.Resp.json(409, "{\"message\":\"No server selected\"}"));
        assertThatThrownBy(() -> client.server().status()).isInstanceOf(ConflictException.class)
                .hasMessageContaining("No server selected");
    }

    @Test
    void exceptionTextDoesNotLeakTheKey() {
        mock.enqueue(MockApi.Resp.json(401, "{\"success\":false,\"error\":\"Invalid API key\"}"));
        Throwable t = catchThrowable(() -> client.server().status());
        assertThat(t.toString()).doesNotContain(MockApi.KEY);
    }

    @Test
    void unreachableHostIsConnectionError() {
        int port;
        try (MockApi dead = new MockApi()) {
            port = Integer.parseInt(dead.baseUrl().substring(dead.baseUrl().lastIndexOf(':') + 1));
        }
        try (TxApi c = TxApi.builder().apiKey("k").baseUrl("http://127.0.0.1:" + port).env(k -> null).maxRetries(0).build()) {
            assertThatThrownBy(() -> c.server().status()).isInstanceOf(ApiConnectionException.class);
        }
    }
}
