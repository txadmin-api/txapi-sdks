package cloud.txapi.sdk.webhooks;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Optional;

/**
 * A verified webhook delivery.
 *
 * @param id        unique delivery id (UUID) — stays the same when a delivery is retried, so it
 *                  is the key for deduplication
 * @param event     event name, one of {@link WebhookEvents} (new names may appear)
 * @param createdAt when the event happened, {@code null} if the field was missing
 * @param serverId  txAdmin server the event belongs to, {@code null} for account events
 *                  (e.g. {@code quota.warning})
 * @param data      event payload; for API-triggered events {@code data.request} is the masked
 *                  request body. Never {@code null} (an empty object when absent)
 */
public record WebhookEvent(String id, String event, Instant createdAt, Long serverId, JsonNode data) {

    /** @return {@link #serverId()} as an Optional */
    public Optional<Long> server() {
        return Optional.ofNullable(serverId);
    }

    /**
     * @param name event name
     * @return whether this is that event
     */
    public boolean is(String name) {
        return name != null && name.equals(event);
    }
}
