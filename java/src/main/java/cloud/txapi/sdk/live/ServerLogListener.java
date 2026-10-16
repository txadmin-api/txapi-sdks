package cloud.txapi.sdk.live;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;

/**
 * Callbacks of the live server log ({@code /ws/serverlog}). Every method has an empty default.
 * Entries are passed through from txAdmin as raw JSON: usually objects such as
 * {@code {"ts":…,"type":"playerJoining","src":{…},"msg":"…"}}, sometimes plain strings.
 *
 * <p>Callbacks run on the HTTP client's executor, one at a time per stream and in frame order.
 */
public interface ServerLogListener {

    /**
     * The connection is (re-)established.
     *
     * @param log the stream
     */
    default void onOpen(LiveServerLog log) {
    }

    /**
     * The buffered entries sent right after connecting — and again after every reconnect.
     *
     * @param entries buffered entries, oldest first
     */
    default void onInit(List<JsonNode> entries) {
    }

    /**
     * One new log entry.
     *
     * @param entry the entry (object or string node)
     */
    default void onEntry(JsonNode entry) {
    }

    /**
     * An {@code error} frame from the server.
     *
     * @param message the error text
     */
    default void onError(String message) {
    }

    /**
     * Every frame as raw JSON, before the typed callbacks.
     *
     * @param frame the frame
     */
    default void onFrame(JsonNode frame) {
    }

    /**
     * The connection was lost and will be re-established after {@code delay}.
     *
     * @param attempt reconnect attempt, starting at 1
     * @param delay   wait before the attempt
     */
    default void onReconnecting(int attempt, Duration delay) {
    }

    /**
     * The stream ended for good.
     *
     * @param code   close code, see {@link LiveClose#code()}
     * @param reason close reason
     */
    default void onClose(int code, String reason) {
    }
}
