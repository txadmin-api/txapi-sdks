package cloud.txapi.sdk.resources;

import cloud.txapi.sdk.AsyncPageIterable;
import cloud.txapi.sdk.PageIterable;
import cloud.txapi.sdk.RequestOptions;
import cloud.txapi.sdk.internal.Call;
import cloud.txapi.sdk.internal.Endpoint;
import cloud.txapi.sdk.internal.Invoker;
import cloud.txapi.sdk.internal.Pager;
import cloud.txapi.sdk.internal.Tokens;
import cloud.txapi.sdk.live.*;
import cloud.txapi.sdk.model.*;
import cloud.txapi.sdk.params.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Server log (joins, deaths, chat, admin actions as txAdmin records them) and the live server log. Every method returns a {@link CompletableFuture} and never blocks the caller.
 *
 * <p>Obtain it from {@code client.async().serverLog()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see ServerLog
 */
@SuppressWarnings("unused")
public final class AsyncServerLog {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public AsyncServerLog(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * The last server-log entries (up to 500), as a snapshot.
     *
     * <p>{@code GET /api/v1/server/log} · permission {@code logs.view}
     *
     * <p>Asynchronous variant of {@link ServerLog#buffer}; errors complete the future exceptionally.
     *
     * @return a future completing with the buffered entries
     */
    public CompletableFuture<ServerLogBuffer> buffer() {
        return inv.callAsync(Calls.serverLog_buffer(), null);
    }

    /**
     * The last server-log entries (up to 500), as a snapshot.
     *
     * <p>{@code GET /api/v1/server/log} · permission {@code logs.view}
     *
     * <p>Asynchronous variant of {@link ServerLog#buffer}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the buffered entries
     */
    public CompletableFuture<ServerLogBuffer> buffer(RequestOptions options) {
        return inv.callAsync(Calls.serverLog_buffer(), options);
    }

    /**
     * The newest page of the stored server log.
     *
     * <p>{@code GET /api/v1/server/log/history} · permission {@code logs.view}
     *
     * <p>Asynchronous variant of {@link ServerLog#history}; errors complete the future exceptionally.
     *
     * @return a future completing with one page of log entries
     */
    public CompletableFuture<ServerLogHistory> history() {
        return inv.callAsync(Calls.serverLog_hist0(), null);
    }

    /**
     * The newest page of the stored server log.
     *
     * <p>{@code GET /api/v1/server/log/history} · permission {@code logs.view}
     *
     * <p>Asynchronous variant of {@link ServerLog#history}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with one page of log entries
     */
    public CompletableFuture<ServerLogHistory> history(RequestOptions options) {
        return inv.callAsync(Calls.serverLog_hist0(), options);
    }

    /**
     * A page of the stored server log before or after a point in time.
     *
     * <p>{@code GET /api/v1/server/log/history} · permission {@code logs.view}
     *
     * <p>Asynchronous variant of {@link ServerLog#history}; errors complete the future exceptionally.
     *
     * @param direction {@code OLDER} (default) or {@code NEWER} than {@code ref}; {@code null} = API default
     * @param ref reference timestamp in epoch milliseconds (e.g. the {@code ts} of the oldest entry you have); {@code null} = now
     * @return a future completing with one page of log entries
     */
    public CompletableFuture<ServerLogHistory> history(LogDirection direction, Long ref) {
        return inv.callAsync(Calls.serverLog_hist1(direction, ref), null);
    }

    /**
     * A page of the stored server log before or after a point in time.
     *
     * <p>{@code GET /api/v1/server/log/history} · permission {@code logs.view}
     *
     * <p>Asynchronous variant of {@link ServerLog#history}; errors complete the future exceptionally.
     *
     * @param direction {@code OLDER} (default) or {@code NEWER} than {@code ref}; {@code null} = API default
     * @param ref reference timestamp in epoch milliseconds (e.g. the {@code ts} of the oldest entry you have); {@code null} = now
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with one page of log entries
     */
    public CompletableFuture<ServerLogHistory> history(LogDirection direction, Long ref, RequestOptions options) {
        return inv.callAsync(Calls.serverLog_hist1(direction, ref), options);
    }

    /**
     * Opens the live server log without blocking; the future completes once the WebSocket is connected.
     * Reconnect behaviour as in {@link ServerLog#connect(ServerLogListener)}.
     *
     * <p>{@code WS /ws/serverlog} · permission {@code logs.view}
     *
     * @param listener callbacks
     * @return completes with the connected stream, or fails with a {@link cloud.txapi.sdk.errors.TxApiException}
     */
    public CompletableFuture<LiveServerLog> connect(ServerLogListener listener) {
        return connect(listener, null);
    }

    /**
     * Opens the live server log with explicit {@link LiveOptions} without blocking.
     *
     * <p>{@code WS /ws/serverlog} · permission {@code logs.view}
     *
     * @param listener callbacks
     * @param options  stream options; {@code null} = defaults
     * @return completes with the connected stream
     */
    public CompletableFuture<LiveServerLog> connect(ServerLogListener listener, LiveOptions options) {
        LiveOptions o = options == null ? LiveOptions.defaults() : options;
        if (o.serverId() == null && inv.defaults().serverId().isPresent()) {
            o = o.serverId(inv.defaults().serverId().getAsLong());
        }
        return LiveServerLog.connectAsync(inv.core(), listener, o);
    }
}
