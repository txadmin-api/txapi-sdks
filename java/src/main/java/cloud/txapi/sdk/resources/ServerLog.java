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
 * Server log (joins, deaths, chat, admin actions as txAdmin records them) and the live server log.
 *
 * <p>Obtain it from {@code client.serverLog()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncServerLog
 */
@SuppressWarnings("unused")
public final class ServerLog {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public ServerLog(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * The last server-log entries (up to 500), as a snapshot.
     *
     * <p>{@code GET /api/v1/server/log} · permission {@code logs.view}
     *
     * @return the buffered entries
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerLogBuffer buffer() {
        return inv.call(Calls.serverLog_buffer(), null);
    }

    /**
     * The last server-log entries (up to 500), as a snapshot.
     *
     * <p>{@code GET /api/v1/server/log} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the buffered entries
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerLogBuffer buffer(RequestOptions options) {
        return inv.call(Calls.serverLog_buffer(), options);
    }

    /**
     * The newest page of the stored server log.
     *
     * <p>{@code GET /api/v1/server/log/history} · permission {@code logs.view}
     *
     * @return one page of log entries
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerLogHistory history() {
        return inv.call(Calls.serverLog_hist0(), null);
    }

    /**
     * The newest page of the stored server log.
     *
     * <p>{@code GET /api/v1/server/log/history} · permission {@code logs.view}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of log entries
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerLogHistory history(RequestOptions options) {
        return inv.call(Calls.serverLog_hist0(), options);
    }

    /**
     * A page of the stored server log before or after a point in time.
     *
     * <p>{@code GET /api/v1/server/log/history} · permission {@code logs.view}
     *
     * @param direction {@code OLDER} (default) or {@code NEWER} than {@code ref}; {@code null} = API default
     * @param ref reference timestamp in epoch milliseconds (e.g. the {@code ts} of the oldest entry you have); {@code null} = now
     * @return one page of log entries
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerLogHistory history(LogDirection direction, Long ref) {
        return inv.call(Calls.serverLog_hist1(direction, ref), null);
    }

    /**
     * A page of the stored server log before or after a point in time.
     *
     * <p>{@code GET /api/v1/server/log/history} · permission {@code logs.view}
     *
     * @param direction {@code OLDER} (default) or {@code NEWER} than {@code ref}; {@code null} = API default
     * @param ref reference timestamp in epoch milliseconds (e.g. the {@code ts} of the oldest entry you have); {@code null} = now
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of log entries
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerLogHistory history(LogDirection direction, Long ref, RequestOptions options) {
        return inv.call(Calls.serverLog_hist1(direction, ref), options);
    }

    /**
     * Opens the live server log and blocks until the WebSocket is connected. It reconnects automatically
     * after a lost connection (backoff 1 s → 30 s) and stops for good on close code 1008 (invalid key,
     * missing permission, used-up quota — the reason goes to {@code onClose}), 1009 or {@link LiveServerLog#close()}.
     * Closing the client closes the stream too.
     *
     * <p>{@code WS /ws/serverlog} · permission {@code logs.view}
     *
     * @param listener callbacks
     * @return the connected stream; close it when done
     * @throws cloud.txapi.sdk.errors.TxApiException if the first connect fails (no reconnect then)
     */
    public LiveServerLog connect(ServerLogListener listener) {
        return connect(listener, null);
    }

    /**
     * Opens the live server log with explicit {@link LiveOptions} (other server, reconnect and timeout
     * settings) and blocks until it is connected.
     *
     * <p>{@code WS /ws/serverlog} · permission {@code logs.view}
     *
     * @param listener callbacks
     * @param options  stream options; {@code null} = defaults (server of this group/client)
     * @return the connected stream; close it when done
     * @throws cloud.txapi.sdk.errors.TxApiException if the first connect fails (no reconnect then)
     */
    public LiveServerLog connect(ServerLogListener listener, LiveOptions options) {
        return LiveServerLog.connect(inv.core(), listener, liveOptions(options));
    }

    private LiveOptions liveOptions(LiveOptions options) {
        LiveOptions o = options == null ? LiveOptions.defaults() : options;
        if (o.serverId() == null && inv.defaults().serverId().isPresent()) {
            o = o.serverId(inv.defaults().serverId().getAsLong());
        }
        return o;
    }
}
