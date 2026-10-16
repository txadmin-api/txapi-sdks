package cloud.txapi.sdk.live;

import cloud.txapi.sdk.errors.CommandException;
import cloud.txapi.sdk.errors.ValidationException;
import cloud.txapi.sdk.internal.ClientCore;
import cloud.txapi.sdk.internal.Json;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The live console ({@code /ws/console}, permission {@code console.write}): receives every console
 * line and runs commands with acknowledgement.
 *
 * <pre>{@code
 * try (LiveConsole console = client.console().connect(new ConsoleListener() {
 *         @Override public void onLine(String line) { System.out.println(line); }
 *     })) {
 *     String ack = console.sendCommand("say hello").join();   // "Command sent"
 * }
 * }</pre>
 *
 * <p>Commands are correlated first-in-first-out: the server answers every command with exactly one
 * {@code ack} or {@code error} frame, in order.
 */
public final class LiveConsole extends LiveStream {
    /** Longest command the server accepts. */
    public static final int MAX_COMMAND_LENGTH = 1024;

    private final ConsoleListener listener;
    private final Deque<Pending> pending = new ArrayDeque<>();

    private static final class Pending {
        final CompletableFuture<String> future = new CompletableFuture<>();
        final String command;
        volatile boolean expired;

        Pending(String command) {
            this.command = command;
        }
    }

    private LiveConsole(ClientCore core, ConsoleListener listener, LiveOptions options) {
        super(core, "/ws/console", "ws.console", options);
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /**
     * Internal: use {@code client.console().connect(...)}.
     *
     * @param core     internal client core
     * @param listener callbacks
     * @param options  stream options, {@code null} = defaults
     * @return the connected stream
     */
    public static LiveConsole connect(ClientCore core, ConsoleListener listener, LiveOptions options) {
        LiveConsole c = new LiveConsole(core, listener, options);
        c.startBlocking();
        return c;
    }

    /**
     * Internal: use {@code client.async().console().connect(...)}.
     *
     * @param core     internal client core
     * @param listener callbacks
     * @param options  stream options, {@code null} = defaults
     * @return completes with the connected stream
     */
    public static CompletableFuture<LiveConsole> connectAsync(ClientCore core, ConsoleListener listener, LiveOptions options) {
        LiveConsole c;
        try {
            c = new LiveConsole(core, listener, options);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        return c.start().thenApply(v -> c);
    }

    /**
     * Runs a console command and waits for the server's acknowledgement — asynchronously.
     *
     * <p>The future completes with the {@code ack} message (e.g. {@code "Command sent"}) or fails with
     * {@link CommandException} on an {@code error} frame, when no acknowledgement arrives within
     * {@link LiveOptions#commandTimeout()} (default 10 s), or when the connection drops first
     * (the command may or may not have run then). A blank or too long command fails with
     * {@link ValidationException} without sending anything. Commands are never re-sent
     * automatically.
     *
     * @param command console command, at most {@value #MAX_COMMAND_LENGTH} characters
     * @return the acknowledgement message
     */
    public CompletableFuture<String> sendCommand(String command) {
        if (command == null || command.isBlank()) {
            return CompletableFuture.failedFuture(new ValidationException("command", "ws.console: 'command' is required and must not be blank"));
        }
        if (command.length() > MAX_COMMAND_LENGTH) {
            return CompletableFuture.failedFuture(new ValidationException("command", "ws.console: 'command' is "
                    + command.length() + " characters long, the API accepts at most " + MAX_COMMAND_LENGTH));
        }
        String json;
        try {
            json = Json.MAPPER.writeValueAsString(Map.of("type", "command", "command", command));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new CommandException("ws.console: could not encode command: " + e));
        }
        Pending p = new Pending(command);
        synchronized (lock) {
            if (!isOpen()) {
                return CompletableFuture.failedFuture(new CommandException(isClosed()
                        ? "ws.console: the console is closed" : "ws.console: not connected (reconnecting)"));
            }
            pending.addLast(p);
            sendText(json).whenComplete((r, t) -> {
                if (t != null) {
                    synchronized (lock) {
                        pending.remove(p);
                    }
                    p.future.completeExceptionally(new CommandException("ws.console: could not send the command: "
                            + ClientCore.unwrap(t).getMessage()));
                }
            });
        }
        Duration timeout = options.commandTimeout();
        ClientCore.later(timeout, () -> {
            // Keep it queued as a placeholder: its late ack/error must not resolve the next command.
            if (p.future.completeExceptionally(new CommandException("ws.console: no acknowledgement for '"
                    + abbreviate(command) + "' within " + timeout.toMillis() + " ms"))) {
                p.expired = true;
            }
        });
        return p.future;
    }

    private static String abbreviate(String s) {
        return s.length() <= 60 ? s : s.substring(0, 57) + "...";
    }

    /** @return number of commands still waiting for their acknowledgement */
    public int pendingCommands() {
        synchronized (lock) {
            return (int) pending.stream().filter(p -> !p.expired).count();
        }
    }

    private Pending nextPending() {
        synchronized (lock) {
            return pending.pollFirst();
        }
    }

    @Override
    void dispatch(String type, JsonNode frame) {
        switch (type) {
            case "init" -> listener.onInit(strings(frame.path("lines")));
            case "log" -> {
                for (String line : strings(frame.path("lines"))) {
                    listener.onLine(line);
                }
            }
            case "ack" -> {
                Pending p = nextPending();
                if (p != null && !p.expired) {
                    p.future.complete(frame.path("message").asText(""));
                }
            }
            case "error" -> {
                String message = frame.path("message").asText("");
                Pending p = nextPending();
                if (p == null) {
                    listener.onError(message);
                } else if (p.expired || !p.future.completeExceptionally(new CommandException(message))) {
                    listener.onError(message);
                }
            }
            default -> {
                // unknown frame type: only onFrame saw it
            }
        }
    }

    static List<String> strings(JsonNode array) {
        List<String> out = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode n : array) {
                out.add(n.isTextual() ? n.textValue() : n.toString());
            }
        }
        return out;
    }

    @Override
    void connectionLost(String why) {
        List<Pending> failed;
        synchronized (lock) {
            failed = new ArrayList<>(pending);
            pending.clear();
        }
        for (Pending p : failed) {
            p.future.completeExceptionally(new CommandException("ws.console: connection lost before the acknowledgement ("
                    + why + "); the command may or may not have run"));
        }
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
