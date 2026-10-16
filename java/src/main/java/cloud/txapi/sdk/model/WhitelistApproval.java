package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * An identifier approved in advance (passes the whitelist on first connect).
 *
 * @param identifier   typed identifier, e.g. {@code discord:272800190639898628}
 * @param playerName   name, {@code unknown} until the player connected
 * @param playerAvatar avatar URL, if any
 * @param tsApproved   approval time, Unix seconds
 * @param approvedBy   admin who approved it
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhitelistApproval(String identifier, String playerName, String playerAvatar, Long tsApproved,
                                String approvedBy) {

    /** {@code tsApproved} as an {@link Instant}, or {@code null}. */
    public Instant approvedAt() {
        return tsApproved == null ? null : Instant.ofEpochSecond(tsApproved);
    }
}
