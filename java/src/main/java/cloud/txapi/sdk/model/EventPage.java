package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Optional;

/**
 * A page of stored events, newest first.
 *
 * @param success    always {@code true}
 * @param serverId   server the events belong to
 * @param eventLog   whether the event log is enabled for the server
 * @param chatLog    chat endpoint only: whether chat logging is enabled (if {@code false}, an empty
 *                   list does not mean nobody wrote)
 * @param count      events on this page
 * @param nextCursor cursor of the next page, {@code null} on the last page
 * @param events     the events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventPage(boolean success, Long serverId, Boolean eventLog, Boolean chatLog, Integer count,
                        String nextCursor, List<ServerEvent> events) {
    public EventPage {
        events = Lists.of(events);
    }

    /** The cursor of the next page, empty on the last page. */
    public Optional<String> next() {
        return Optional.ofNullable(nextCursor).filter(s -> !s.isBlank());
    }
}
