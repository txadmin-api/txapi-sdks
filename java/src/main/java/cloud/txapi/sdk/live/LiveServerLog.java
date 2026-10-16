package cloud.txapi.sdk.live;

import cloud.txapi.sdk.internal.ClientCore;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The live server log ({@code /ws/serverlog}, permission {@code logs.view}): joins, deaths, chat,
 * menu actions … as txAdmin records them. Read-only.
 *
 * <pre>{@code
 * LiveServerLog log = client.serverLog().connect(new ServerLogListener() {
 *     @Override public void onEntry(JsonNode e) { System.out.println(e.path("msg").asText(e.toString())); }
 * });
 * }</pre>
 */
public final class LiveServerLog extends LiveStream {
    private final ServerLogListener listener;

    private LiveServerLog(ClientCore core, ServerLogListener listener, LiveOptions options) {
        super(core, "/ws/serverlog", "ws.serverlog", options);
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Internal: use {@code client.serverLog().connect(...)}.
     *
     * @param core     internal client core
     * @param listener callbacks
     * @param options  stream options, {@code null} = defaults
     * @return the connected stream
     */
    public static LiveServerLog connect(ClientCore core, ServerLogListener listener, LiveOptions options) {
        LiveServerLog l = new LiveServerLog(core, listener, options);
        l.startBlocking();
        return l;
    }

    /**
     * Internal: use {@code client.async().serverLog().connect(...)}.
     *
     * @param core     internal client core
     * @param listener callbacks
     * @param options  stream options, {@code null} = defaults
     * @return completes with the connected stream
     */
    public static CompletableFuture<LiveServerLog> connectAsync(ClientCore core, ServerLogListener listener, LiveOptions options) {
        LiveServerLog l;
        try {
            l = new LiveServerLog(core, listener, options);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        return l.start().thenApply(v -> l);
    }

    @Override
    void dispatch(String type, JsonNode frame) {
        switch (type) {
            case "init" -> listener.onInit(entries(frame.path("entries")));
            case "log" -> {
                for (JsonNode e : entries(frame.path("entries"))) {
                    listener.onEntry(e);
                }
            }
            case "error" -> listener.onError(frame.path("message").asText(""));
            default -> {
                // unknown frame type: only onFrame saw it
            }
        }
    }

    private static List<JsonNode> entries(JsonNode array) {
        List<JsonNode> out = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(out::add);
        }
        return out;
    }

    @Override
    void fireOpen() {
        listener.onOpen(this);
    }

    @Override
    void fireFrame(JsonNode frame) {
        listener.onFrame(frame);
    }

    @Override
    void fireReconnecting(int attempt, Duration delay) {
        listener.onReconnecting(attempt, delay);
    }

    @Override
    void fireClose(int code, String reason) {
        listener.onClose(code, reason);
    }
}
