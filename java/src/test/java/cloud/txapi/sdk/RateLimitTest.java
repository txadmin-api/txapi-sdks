package cloud.txapi.sdk;

import cloud.txapi.sdk.model.ServerStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitTest {
    private MockApi mock;

    @BeforeEach
    void setUp() {
        mock = new MockApi().handler(r -> MockApi.Resp.ok("{\"online\":true}")
                .header("X-RateLimit-Limit", "120").header("X-RateLimit-Remaining", String.valueOf(120 - 1))
                .header("X-RateLimit-Reset", "42"));
    }

    @AfterEach
    void tearDown() {
        mock.close();
    }

    @Test
    void lastRateLimitIsTracked() {
        try (TxApi c = mock.client()) {
            assertThat(c.lastRateLimit()).isEmpty();
            c.server().status();
            assertThat(c.lastRateLimit()).hasValue(new RateLimit(120L, 119L, 42L));
            assertThat(c.forServer(3).lastRateLimit()).hasValue(new RateLimit(120L, 119L, 42L));
            assertThat(c.async().lastRateLimit()).isPresent();
        }
    }

    @Test
    void rawResponseCarriesStatusHeadersAndRateLimit() {
        try (TxApi c = mock.client()) {
            ApiResponse<ServerStatus> r = c.withRawResponse(x -> x.server().status());
            assertThat(r.status()).isEqualTo(200);
            assertThat(r.data().online()).isTrue();
            assertThat(r.rateLimit()).hasValue(new RateLimit(120L, 119L, 42L));
            assertThat(r.header("x-ratelimit-reset")).hasValue("42");
            assertThat(r.header("X-RATELIMIT-RESET")).hasValue("42");

            ApiResponse<ServerStatus> a = c.async().withRawResponse(x -> x.server().status()).join();
            assertThat(a.status()).isEqualTo(200);
            assertThat(a.data().online()).isTrue();
            assertThat(a.rateLimit()).isPresent();

            assertThatThrownBy(() -> c.withRawResponse(x -> "no call")).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void hooksSeeEveryAttempt() {
        List<RequestEvent> requests = new CopyOnWriteArrayList<>();
        List<ResponseEvent> responses = new CopyOnWriteArrayList<>();
        try (TxApi c = mock.client(b -> b.onRequest(requests::add).onResponse(responses::add))) {
            c.server().status();
        }
        assertThat(requests).singleElement().satisfies(e -> {
            assertThat(e.endpointId()).isEqualTo("server.status");
            assertThat(e.method()).isEqualTo("GET");
            assertThat(e.uri().getPath()).isEqualTo("/api/v1/server/status");
        });
        assertThat(responses).singleElement().satisfies(e -> {
            assertThat(e.status()).isEqualTo(200);
            assertThat(e.rateLimit().remaining()).isEqualTo(119L);
            assertThat(e.request().endpointId()).isEqualTo("server.status");
        });
    }

    @Test
    void throwingHooksNeverBreakARequest() {
        try (TxApi c = mock.client(b -> b.onRequest(e -> { throw new IllegalStateException("hook"); })
                .onResponse(e -> { throw new IllegalStateException("hook"); }))) {
            assertThat(c.server().status().online()).isTrue();
        }
    }
}
