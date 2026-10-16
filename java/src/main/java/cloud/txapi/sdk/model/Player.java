package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * A row of txAdmin's player database.
 *
 * @param license          license hash (without {@code license:})
 * @param displayName      last known name
 * @param playTime         play time in minutes
 * @param tsJoined         first join, Unix seconds
 * @param tsLastConnection last connection, Unix seconds
 * @param notes            staff note
 * @param admin            whether the player is a txAdmin admin
 * @param online           whether the player is connected right now
 * @param whitelisted      whether the player is whitelisted
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Player(String license, String displayName, Long playTime, Long tsJoined, Long tsLastConnection,
                     String notes,
                     @JsonProperty("isAdmin") Boolean admin,
                     @JsonProperty("isOnline") Boolean online,
                     @JsonProperty("isWhitelisted") Boolean whitelisted) {

    /** {@code tsJoined} as an {@link Instant}, or {@code null}. */
    public Instant joinedAt() {
        return tsJoined == null ? null : Instant.ofEpochSecond(tsJoined);
    }

    /** {@code tsLastConnection} as an {@link Instant}, or {@code null}. */
    public Instant lastConnectionAt() {
        return tsLastConnection == null ? null : Instant.ofEpochSecond(tsLastConnection);
    }
}
