package cloud.txapi.sdk.live;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;

/**
 * Callbacks of the live console ({@code /ws/console}). Every method has an empty default, so
 * implement only what you need — usually as a lambda-friendly anonymous class:
 *
 * <pre>{@code
 * LiveConsole console = client.console().connect(new ConsoleListener() {
 *     @Override public void onLine(String line) { System.out.println(line); }
 *     @Override public void onClose(int code, String reason) { System.err.println("closed: " + reason); }
 * });
 * }</pre>
 *
 * <p>Callbacks run on the HTTP client's executor, one at a time per stream and in frame order.
 * Do not block in them; hand longer work to your own executor.
 */
public interface ConsoleListener {

    /**
     * The connection is (re-)established.
     *
     * @param console the stream
     */
    default void onOpen(LiveConsole console) {
    }

    /**
     * The scrollback the server sends right after connecting — and again after every reconnect
     * (the full buffer, so it may repeat lines you already have; de-duplicate or ignore it).
     *
     * @param lines buffered console lines, oldest first
     */
    default void onInit(List<String> lines) {
    }

    /**
     * One new console line.
     *
     * @param line the line (may contain ANSI colour codes)
     */
    default void onLine(String line) {
    }

    /**
     * An {@code error} frame that does not belong to a command sent with
     * {@link LiveConsole#sendCommand(String)} (e.g. the server could not set up the stream).
     *
     * @param message the error text
     */
    default void onError(String message) {
    }

    /**
     * Every frame as raw JSON, before the typed callbacks — for frame types this SDK does not know.
     *
     * @param frame the frame
     */
    default void onFrame(JsonNode frame) {
    }

    /**
     * The connection was lost and will be re-established after {@code delay}.
     *
     * @param attempt reconnect attempt, starting at 1 (reset after a successful reconnect)
     * @param delay   wait before the attempt
     */
    default void onReconnecting(int attempt, Duration delay) {
    }

    /**
     * The stream ended for good (no further callbacks follow). Not called when the very first
     * connect fails — that throws from {@code connect(...)} instead.
     *
     * @param code   close code, see {@link LiveClose#code()}
     * @param reason close reason
     */
    default void onClose(int code, String reason) {
    }
}
