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
 * Server console: buffer, single commands and the live console ({@link #connect}). Every method returns a {@link CompletableFuture} and never blocks the caller.
 *
 * <p>Obtain it from {@code client.async().console()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see Console
 */
@SuppressWarnings("unused")
public final class AsyncConsole {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public AsyncConsole(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * The last console lines (up to 500), as a snapshot.
     *
     * <p>{@code GET /api/v1/console} · permission {@code console.write}
     *
     * <p>Asynchronous variant of {@link Console#buffer}; errors complete the future exceptionally.
     *
     * @return a future completing with the buffered lines
     */
    public CompletableFuture<ConsoleBuffer> buffer() {
        return inv.callAsync(Calls.console_buffer(), null);
    }

    /**
     * The last console lines (up to 500), as a snapshot.
     *
     * <p>{@code GET /api/v1/console} · permission {@code console.write}
     *
     * <p>Asynchronous variant of {@link Console#buffer}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the buffered lines
     */
    public CompletableFuture<ConsoleBuffer> buffer(RequestOptions options) {
        return inv.callAsync(Calls.console_buffer(), options);
    }

    /**
     * Runs one console command. For many commands keep a live console open instead ({@link #connect}).
     *
     * <p>{@code POST /api/v1/console/command} · permission {@code console.write}
     *
     * <p>Asynchronous variant of {@link Console#command}; errors complete the future exceptionally.
     *
     * @param command the command, at most 1024 characters
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> command(String command) {
        return inv.callAsync(Calls.console_command(command), null);
    }

    /**
     * Runs one console command. For many commands keep a live console open instead ({@link #connect}).
     *
     * <p>{@code POST /api/v1/console/command} · permission {@code console.write}
     *
     * <p>Asynchronous variant of {@link Console#command}; errors complete the future exceptionally.
     *
     * @param command the command, at most 1024 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> command(String command, RequestOptions options) {
        return inv.callAsync(Calls.console_command(command), options);
    }

    /**
     * Opens the live console without blocking; the future completes once the WebSocket is connected.
     * Reconnect behaviour as in {@link Console#connect(ConsoleListener)}.
     *
     * <p>{@code WS /ws/console} · permission {@code console.write}
     *
     * @param listener callbacks
     * @return completes with the connected stream, or fails with a {@link cloud.txapi.sdk.errors.TxApiException}
     */
    public CompletableFuture<LiveConsole> connect(ConsoleListener listener) {
        return connect(listener, null);
    }

    /**
     * Opens the live console with explicit {@link LiveOptions} without blocking.
     *
     * <p>{@code WS /ws/console} · permission {@code console.write}
     *
     * @param listener callbacks
     * @param options  stream options; {@code null} = defaults
     * @return completes with the connected stream
     */
    public CompletableFuture<LiveConsole> connect(ConsoleListener listener, LiveOptions options) {
        LiveOptions o = options == null ? LiveOptions.defaults() : options;
        if (o.serverId() == null && inv.defaults().serverId().isPresent()) {
            o = o.serverId(inv.defaults().serverId().getAsLong());
        }
        return LiveConsole.connectAsync(inv.core(), listener, o);
    }
}
