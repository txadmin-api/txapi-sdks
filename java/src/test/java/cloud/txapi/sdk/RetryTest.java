package cloud.txapi.sdk;

import cloud.txapi.sdk.errors.ApiConnectionException;
import cloud.txapi.sdk.errors.RateLimitException;
import cloud.txapi.sdk.errors.ServerException;
import cloud.txapi.sdk.errors.TxAdminUnavailableException;
import cloud.txapi.sdk.internal.ClientCore;
import cloud.txapi.sdk.params.BanRequest;
import cloud.txapi.sdk.model.ActionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryTest {
    private MockApi mock;

    @BeforeEach
    void setUp() {
        mock = new MockApi();
    }

    @AfterEach
    void tearDown() {
        mock.close();
    }

    private static MockApi.Resp tooMany(String retryAfter) {
        MockApi.Resp r = MockApi.Resp.json(429, "{\"success\":false,\"error\":\"Rate limit exceeded\"}");
        return retryAfter == null ? r : r.header("Retry-After", retryAfter);
    }

    @Test
    void retries429WithRetryAfterEvenForPost() {
        mock.enqueue(tooMany("0"), MockApi.Resp.ok("{\"success\":true,\"message\":\"banned\"}"));
        List<Integer> attempts = new CopyOnWriteArrayList<>();
        try (TxApi c = mock.client(b -> b.onRequest(e -> attempts.add(e.attempt())))) {
            ActionResult r = c.players().ban(BanRequest.of("license:abc", "cheating"));
            assertThat(r.success()).isTrue();
        }
        assertThat(mock.requests).hasSize(2);
        assertThat(mock.requests).allSatisfy(r -> assertThat(r.method()).isEqualTo("POST"));
        assertThat(attempts).containsExactly(0, 1);
    }

    @Test
    void waitsForRetryAfter() {
        mock.enqueue(tooMany("1"), MockApi.Resp.ok("{}"));
        long start = System.nanoTime();
        try (TxApi c = mock.client()) {
            c.server().status();
        }
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(950));
        assertThat(mock.requests).hasSize(2);
    }

    @Test
    void quotaExhaustedIsNotRetried() {
        mock.enqueue(tooMany(null), MockApi.Resp.ok("{}"));
        try (TxApi c = mock.client()) {
            assertThatThrownBy(() -> c.server().status()).isInstanceOf(RateLimitException.class);
        }
        assertThat(mock.requests).hasSize(1);
    }

    @Test
    void longRetryAfterIsNotWaitedFor() {
        mock.enqueue(tooMany("120"), MockApi.Resp.ok("{}"));
        try (TxApi c = mock.client()) {
            assertThatThrownBy(() -> c.server().status()).isInstanceOf(RateLimitException.class);
        }
        assertThat(mock.requests).hasSize(1);
    }

    @Test
    void retries502ForGetButNotPost() {
        mock.handler(r -> MockApi.Resp.json(502, "{\"success\":false,\"error\":\"txAdmin unreachable\"}"));
        try (TxApi c = mock.client()) {
            assertThatThrownBy(() -> c.server().status()).isInstanceOf(TxAdminUnavailableException.class);
            assertThat(mock.requests).hasSize(3); // 1 + DEFAULT_MAX_RETRIES (2)

            mock.requests.clear();
            assertThatThrownBy(() -> c.server().restart()).isInstanceOf(TxAdminUnavailableException.class);
            assertThat(mock.requests).hasSize(1);
        }
    }

    @Test
    void retries503And504ForGet() {
        mock.enqueue(MockApi.Resp.json(503, "{}"), MockApi.Resp.json(504, "{}"), MockApi.Resp.ok("{\"online\":true}"));
        try (TxApi c = mock.client()) {
            assertThat(c.server().status().online()).isTrue();
        }
        assertThat(mock.requests).hasSize(3);
    }

    @Test
    void doesNotRetryOther5xx() {
        mock.enqueue(MockApi.Resp.json(500, "{}"), MockApi.Resp.ok("{}"));
        try (TxApi c = mock.client()) {
            assertThatThrownBy(() -> c.server().status()).isInstanceOf(ServerException.class);
        }
        assertThat(mock.requests).hasSize(1);
    }

    @Test
    void maxRetriesZeroDisablesRetries() {
        mock.handler(r -> MockApi.Resp.json(502, "{}"));
        try (TxApi c = mock.client(b -> b.maxRetries(0))) {
            assertThatThrownBy(() -> c.server().status()).isInstanceOf(TxAdminUnavailableException.class);
            assertThat(mock.requests).hasSize(1);

            mock.requests.clear();
            assertThatThrownBy(() -> c.server().status(RequestOptions.builder().maxRetries(1).build()))
                    .isInstanceOf(TxAdminUnavailableException.class);
            assertThat(mock.requests).hasSize(2); // per-call option wins
        }
        mock.requests.clear();
        mock.handler(r -> tooMany("0"));
        try (TxApi c = mock.client(b -> b.maxRetries(0))) {
            assertThatThrownBy(() -> c.players().ban("license:a", "x")).isInstanceOf(RateLimitException.class);
        }
        assertThat(mock.requests).hasSize(1);
    }

    @Test
    void asyncRetriesToo() {
        mock.enqueue(MockApi.Resp.json(502, "{}"), tooMany("0"), MockApi.Resp.ok("{\"online\":true}"));
        try (TxApi c = mock.client()) {
            assertThat(c.async().server().status().join().online()).isTrue();
        }
        assertThat(mock.requests).hasSize(3);
    }

    @Test
    void transportErrorsAreRetriedForGetOnly() {
        int port;
        try (MockApi dead = new MockApi()) {
            port = Integer.parseInt(dead.baseUrl().substring(dead.baseUrl().lastIndexOf(':') + 1));
        }
        List<String> attempts = new CopyOnWriteArrayList<>();
        try (TxApi c = TxApi.builder().apiKey("k").baseUrl("http://127.0.0.1:" + port).env(k -> null)
                .maxRetries(2).onRequest(e -> attempts.add(e.method() + e.attempt())).build()) {
            assertThatThrownBy(() -> c.server().status()).isInstanceOf(ApiConnectionException.class);
            assertThat(attempts).containsExactly("GET0", "GET1", "GET2");
            attempts.clear();
            assertThatThrownBy(() -> c.server().restart()).isInstanceOf(ApiConnectionException.class);
            assertThat(attempts).containsExactly("POST0");
        }
    }

    @Test
    void backoffStaysWithinCap() {
        for (int attempt = 0; attempt < 12; attempt++) {
            Duration d = ClientCore.backoff(attempt);
            long cap = Math.min(8_000L, 500L << attempt);
            assertThat(d.toMillis()).isBetween(0L, cap);
        }
    }
}
