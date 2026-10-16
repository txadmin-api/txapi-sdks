package cloud.txapi.sdk.live;

import cloud.txapi.sdk.errors.ApiConnectionException;
import cloud.txapi.sdk.errors.ApiException;
import cloud.txapi.sdk.errors.ApiTimeoutException;
import cloud.txapi.sdk.errors.RequestCancelledException;
import cloud.txapi.sdk.errors.TxApiException;
import cloud.txapi.sdk.internal.ClientCore;
import cloud.txapi.sdk.internal.Errors;
import cloud.txapi.sdk.internal.Json;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A live WebSocket stream with automatic reconnect (SPEC §9). See {@link LiveConsole} and
 * {@link LiveServerLog}.
 *
 * <p>Reconnects after close codes {@code 1001}, {@code 1006}, {@code 1011}, {@code 1012} and network
 * loss, with a backoff from 1 s to 30 s plus jitter. It does <em>not</em> reconnect after
 * {@code 1008} (key invalid, permission or quota missing, client too slow — the reason is passed
 * to {@code onClose}), {@code 1009}, or after {@link #close()}.
 */
public abstract class LiveStream implements AutoCloseable {
    private static final System.Logger LOG = System.getLogger("cloud.txapi.sdk.live");

    /** Close codes after which the stream reconnects. */
    public static final Set<Integer> RECONNECT_CODES = Set.of(1001, 1006, 1011, 1012);

    final ClientCore core;
    final LiveOptions options;
    final String name;
    private final URI uri;

    final Object lock = new Object();
    private final Object callbackLock = new Object();
    private WebSocket socket;
    private int generation;
    private int failures;
    private volatile boolean open;
    private volatile boolean userClosed;
    private CompletableFuture<?> sendChain = CompletableFuture.completedFuture(null);
    private final CompletableFuture<Void> firstOpen = new CompletableFuture<>();
    private final CompletableFuture<LiveClose> closed = new CompletableFuture<>();

    LiveStream(ClientCore core, String wsPath, String name, LiveOptions options) {
        this.core = core;
        this.options = options == null ? LiveOptions.defaults() : options;
        this.name = name;
        this.uri = core.webSocketUri(wsPath, this.options.serverId());
    }

    // ── subclass hooks ────────────────────────────────────────────────────────

    /** Typed handling of one parsed frame (called with the callback lock held). */
    abstract void dispatch(String type, JsonNode frame);

    abstract void fireOpen();

    abstract void fireFrame(JsonNode frame);

    abstract void fireReconnecting(int attempt, Duration delay);

    abstract void fireClose(int code, String reason);

    /** The current connection is gone (reconnect or final close). */
    void connectionLost(String why) {
    }

    // ── public API ────────────────────────────────────────────────────────────

    /** @return {@code true} while connected (not while reconnecting or after the end) */
    public boolean isOpen() {
        return open;
    }

    /** @return {@code true} once the stream ended for good */
    public boolean isClosed() {
        return closed.isDone();
    }

    /**
     * @return completes once the stream ended for good (after {@link #close()}, a non-reconnect close
     *         code, or when reconnecting gave up)
     */
    public CompletableFuture<LiveClose> closeFuture() {
        return closed.thenApply(c -> c);
    }

    /**
     * Blocks until the stream ended for good.
     *
     * @return how it ended
     * @throws RequestCancelledException if the thread is interrupted while waiting
     */
    public LiveClose awaitClose() {
        try {
            return closed.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestCancelledException(name + ": interrupted while waiting for the stream to close", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    /**
     * Blocks until the stream ended for good or the timeout elapsed.
     *
     * @param timeout how long to wait
     * @return how it ended, or {@code null} if it is still running
     * @throws RequestCancelledException if the thread is interrupted while waiting
     */
    public LiveClose awaitClose(Duration timeout) {
        try {
            return closed.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RequestCancelledException(name + ": interrupted while waiting for the stream to close", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    /**
     * Closes the stream (close code {@code 1000}); it does not reconnect afterwards. Pending
     * commands fail with {@link cloud.txapi.sdk.errors.CommandException}. Idempotent.
     */
    @Override
    public void close() {
        WebSocket ws;
        synchronized (lock) {
            if (userClosed) {
                return;
            }
            userClosed = true;
            ws = socket;
        }
        if (ws == null) {
            finish(1000, "closed by client");
            return;
        }
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "closed by client").whenComplete((r, t) -> {
            if (t != null) {
                ws.abort();
                finish(1000, "closed by client");
            }
        });
        // If the server never answers the close frame, give up after 5 s.
        ClientCore.later(Duration.ofSeconds(5), () -> {
            if (!closed.isDone()) {
                ws.abort();
                finish(1000, "closed by client");
            }
        });
    }

    @Override
    public String toString() {
        String safe = uri.toString().replaceAll("apiKey=[^&]*", "apiKey=***");
        return getClass().getSimpleName() + "{" + safe + (open ? ", open" : closed.isDone() ? ", closed" : ", connecting") + "}";
    }

    // ── connecting ────────────────────────────────────────────────────────────

    /** Opens the first connection; the future completes on open or fails with a TxApiException. */
    CompletableFuture<Void> start() {
        core.register(this);
        int gen;
        synchronized (lock) {
            gen = ++generation;
        }
        connect(gen);
        return firstOpen.thenApply(v -> v);
    }

    void startBlocking() {
        try {
            start().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            close();
            throw new RequestCancelledException(name + ": interrupted while connecting", e);
        } catch (ExecutionException e) {
            Throwable c = ClientCore.unwrap(e);
            if (c instanceof TxApiException t) {
                throw t;
            }
            throw new ApiConnectionException(name + ": " + c, c);
        }
    }

    private void connect(int gen) {
        Duration timeout = options.connectTimeout() != null ? options.connectTimeout() : core.config().timeout();
        CompletableFuture<WebSocket> f;
        try {
            f = core.http().newWebSocketBuilder()
                    .header("User-Agent", core.userAgent())
                    .connectTimeout(timeout)
                    .buildAsync(uri, new SocketListener(gen));
        } catch (RuntimeException e) {
            connectFailed(gen, e);
            return;
        }
        f.whenComplete((ws, error) -> {
            if (error != null) {
                connectFailed(gen, ClientCore.unwrap(error));
            }
        });
    }

    private void connectFailed(int gen, Throwable error) {
        synchronized (lock) {
            if (gen != generation) {
                return;
            }
        }
        TxApiException mapped = mapConnectError(error);
        if (!firstOpen.isDone()) {
            // The very first connect: report to the caller of connect(...) and stop.
            firstOpen.completeExceptionally(mapped);
            finishQuietly(1006, mapped.getMessage());
            return;
        }
        boolean permanent = mapped instanceof ApiException api && api.status() >= 400 && api.status() < 500
                && api.status() != 408 && api.status() != 429;
        if (permanent || userClosed) {
            finish(permanent ? 1008 : 1000, mapped.getMessage());
        } else {
            scheduleReconnect(1006, mapped.getMessage());
        }
    }

    TxApiException mapConnectError(Throwable error) {
        if (error instanceof WebSocketHandshakeException hs && hs.getResponse() != null) {
            var response = hs.getResponse();
            Object body = response.body();
            String raw = body instanceof byte[] b ? new String(b, StandardCharsets.UTF_8)
                    : body == null ? "" : String.valueOf(body);
            Map<String, List<String>> headers = ClientCore.lowerCase(response.headers().map());
            return Errors.fromResponse(response.statusCode(), raw, headers, cloud.txapi.sdk.RateLimit.fromHeaders(headers));
        }
        if (error instanceof HttpTimeoutException) {
            return new ApiTimeoutException(name + ": no handshake within the connect timeout", error);
        }
        if (error instanceof TxApiException t) {
            return t;
        }
        String msg = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return new ApiConnectionException(name + ": could not connect: " + msg, error);
    }

    private void scheduleReconnect(int code, String reason) {
        int attempt;
        synchronized (lock) {
            failures++;
            attempt = failures;
        }
        if (!options.reconnect() || (options.maxReconnectAttempts() >= 0 && attempt > options.maxReconnectAttempts())) {
            finish(code, reason);
            return;
        }
        Duration delay = backoff(attempt);
        callback(() -> fireReconnecting(attempt, delay));
        ClientCore.later(delay, () -> {
            int gen;
            synchronized (lock) {
                if (userClosed || closed.isDone()) {
                    return;
                }
                gen = ++generation;
            }
            connect(gen);
        });
    }

    /** Equal jitter: half the exponential delay plus a random share of the other half. */
    Duration backoff(int attempt) {
        long initial = options.initialReconnectDelay().toMillis();
        long max = options.maxReconnectDelay().toMillis();
        long exp = initial << Math.min(attempt - 1, 20);
        long base = Math.max(1, Math.min(max, exp));
        long half = base / 2;
        return Duration.ofMillis(half + ThreadLocalRandom.current().nextLong(base - half + 1));
    }

    private void lost(int gen, int code, String reason) {
        synchronized (lock) {
            if (gen != generation || closed.isDone()) {
                return;
            }
            generation++;
            socket = null;
            open = false;
            sendChain = CompletableFuture.completedFuture(null);
        }
        String why = reason == null || reason.isBlank() ? "close code " + code : reason;
        connectionLost(why);
        if (userClosed) {
            finish(code == 1006 ? 1000 : code, userClosedReason(code, reason));
        } else if (RECONNECT_CODES.contains(code)) {
            scheduleReconnect(code, why);
        } else {
            finish(code, why);
        }
    }

    private static String userClosedReason(int code, String reason) {
        return reason == null || reason.isBlank() ? "closed by client" : reason;
    }

    private void finish(int code, String reason) {
        boolean first;
        synchronized (lock) {
            open = false;
            socket = null;
            first = closed.complete(new LiveClose(code, reason, userClosed));
        }
        if (first) {
            connectionLost(reason);
            core.unregister(this);
            callback(() -> fireClose(code, reason));
        }
    }

    private void finishQuietly(int code, String reason) {
        synchronized (lock) {
            open = false;
            socket = null;
            closed.complete(new LiveClose(code, reason, userClosed));
        }
        core.unregister(this);
    }

    // ── sending ───────────────────────────────────────────────────────────────

    /** Sends a text frame; sends are serialised (the JDK allows one outstanding send). */
    CompletableFuture<Void> sendText(String text) {
        synchronized (lock) {
            WebSocket ws = socket;
            if (ws == null || !open) {
                return CompletableFuture.failedFuture(new IllegalStateException("not connected"));
            }
            CompletableFuture<Void> next = sendChain.handle((r, t) -> null)
                    .thenCompose(x -> ws.sendText(text, true))
                    .thenApply(w -> null);
            sendChain = next;
            return next;
        }
    }

    // ── callbacks ─────────────────────────────────────────────────────────────

    final void callback(Runnable r) {
        synchronized (callbackLock) {
            try {
                r.run();
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, name + ": listener threw; ignored", e);
            }
        }
    }

    private void handleText(int gen, String text) {
        synchronized (lock) {
            if (gen != generation) {
                return;
            }
        }
        JsonNode frame = Json.tryParse(text);
        if (frame == null || !frame.isObject()) {
            LOG.log(System.Logger.Level.DEBUG, () -> name + ": ignoring non-JSON frame");
            return;
        }
        String type = frame.path("type").asText("");
        callback(() -> {
            fireFrame(frame);
            dispatch(type, frame);
        });
    }

    private final class SocketListener implements WebSocket.Listener {
        private final int gen;
        private final StringBuilder text = new StringBuilder();

        SocketListener(int gen) {
            this.gen = gen;
        }

        @Override
        public void onOpen(WebSocket ws) {
            synchronized (lock) {
                if (gen != generation || userClosed || closed.isDone()) {
                    ws.abort();
                    return;
                }
                socket = ws;
                open = true;
                failures = 0;
            }
            ws.request(1);
            callback(LiveStream.this::fireOpen);
            firstOpen.complete(null);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            text.append(data);
            if (last) {
                String message = text.toString();
                text.setLength(0);
                handleText(gen, message);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            lost(gen, statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            String msg = error instanceof IOException && error.getMessage() != null ? error.getMessage()
                    : String.valueOf(error);
            lost(gen, 1006, "connection lost: " + msg);
        }
    }
}
