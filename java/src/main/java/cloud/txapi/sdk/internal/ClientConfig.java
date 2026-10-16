package cloud.txapi.sdk.internal;

import cloud.txapi.sdk.RequestEvent;
import cloud.txapi.sdk.ResponseEvent;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Consumer;

/** Resolved, validated client configuration (built by {@code TxApi.Builder}). */
public record ClientConfig(
        String apiKey,
        String baseUrl,
        Long serverId,
        Duration timeout,
        int maxRetries,
        String userAgent,
        HttpClient httpClient,
        boolean ownsHttpClient,
        Consumer<RequestEvent> onRequest,
        Consumer<ResponseEvent> onResponse) {

    @Override
    public String toString() {
        return "ClientConfig{baseUrl=" + baseUrl + ", serverId=" + serverId + ", timeout=" + timeout
                + ", maxRetries=" + maxRetries + ", apiKey=***}";
    }
}
