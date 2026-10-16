package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * A ban or warning from txAdmin's history.
 *
 * @param id            action id, e.g. {@code BKL5-3SH4}
 * @param type          {@code ban} or {@code warn}
 * @param playerName    player name
 * @param author        admin who issued it
 * @param reason        reason
 * @param timestamp     Unix seconds
 * @param revoked       whether it was revoked
 * @param banExpiration bans only: {@code permanent}, {@code active} or {@code expired}
 * @param warnAcked     warnings only: whether the player acknowledged it
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HistoryEntry(String id, String type, String playerName, String author, String reason, Long timestamp,
                           @JsonProperty("isRevoked") Boolean revoked, String banExpiration, Boolean warnAcked) {

    /** {@code timestamp} as an {@link Instant}, or {@code null}. */
    public Instant time() {
        return timestamp == null ? null : Instant.ofEpochSecond(timestamp);
    }
}
