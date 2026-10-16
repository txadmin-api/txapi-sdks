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
 * Server console: buffer, single commands and the live console ({@link #connect}).
 *
 * <p>Obtain it from {@code client.console()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncConsole
 */
@SuppressWarnings("unused")
public final class Console {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public Console(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * The last console lines (up to 500), as a snapshot.
     *
     * <p>{@code GET /api/v1/console} · permission {@code console.write}
     *
     * @return the buffered lines
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ConsoleBuffer buffer() {
        return inv.call(Calls.console_buffer(), null);
    }

    /**
     * The last console lines (up to 500), as a snapshot.
     *
     * <p>{@code GET /api/v1/console} · permission {@code console.write}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the buffered lines
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ConsoleBuffer buffer(RequestOptions options) {
        return inv.call(Calls.console_buffer(), options);
    }

    /**
     * Runs one console command. For many commands keep a live console open instead ({@link #connect}).
     *
     * <p>{@code POST /api/v1/console/command} · permission {@code console.write}
     *
     * @param command the command, at most 1024 characters
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult command(String command) {
        return inv.call(Calls.console_command(command), null);
    }

    /**
     * Runs one console command. For many commands keep a live console open instead ({@link #connect}).
     *
     * <p>{@code POST /api/v1/console/command} · permission {@code console.write}
     *
     * @param command the command, at most 1024 characters
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult command(String command, RequestOptions options) {
        return inv.call(Calls.console_command(command), options);
    }

    /**
     * Opens the live console and blocks until the WebSocket is connected. It reconnects automatically
     * after a lost connection (backoff 1 s → 30 s) and stops for good on close code 1008 (invalid key,
     * missing permission, used-up quota — the reason goes to {@code onClose}), 1009 or {@link LiveConsole#close()}.
     * Closing the client closes the stream too.
     *
     * <p>{@code WS /ws/console} · permission {@code console.write}
     *
     * @param listener callbacks
     * @return the connected stream; close it when done
     * @throws cloud.txapi.sdk.errors.TxApiException if the first connect fails (no reconnect then)
     */
    public LiveConsole connect(ConsoleListener listener) {
        return connect(listener, null);
    }

    /**
     * Opens the live console with explicit {@link LiveOptions} (other server, reconnect and timeout
     * settings) and blocks until it is connected.
     *
     * <p>{@code WS /ws/console} · permission {@code console.write}
     *
     * @param listener callbacks
     * @param options  stream options; {@code null} = defaults (server of this group/client)
     * @return the connected stream; close it when done
     * @throws cloud.txapi.sdk.errors.TxApiException if the first connect fails (no reconnect then)
     */
    public LiveConsole connect(ConsoleListener listener, LiveOptions options) {
        return LiveConsole.connect(inv.core(), listener, liveOptions(options));
    }

    private LiveOptions liveOptions(LiveOptions options) {
        LiveOptions o = options == null ? LiveOptions.defaults() : options;
        if (o.serverId() == null && inv.defaults().serverId().isPresent()) {
            o = o.serverId(inv.defaults().serverId().getAsLong());
        }
        return o;
    }
}
