package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * One stored event of the event log. Which fields are set depends on {@code type}:
 * <ul>
 *   <li>{@code death}: victim, victimRef, killer, killerRef, cause, pvp, killerUnknown</li>
 *   <li>{@code ban}/{@code warn}: actionId, player, admin, reason, expiration (ban), revoked</li>
 *   <li>{@code admin}: admin, message, kind, loggedTime — {@code menu}: admin, message</li>
 *   <li>{@code join}: player, playerRef, session, identifiers</li>
 *   <li>{@code leave}/{@code join_denied}: player, playerRef, session, reason</li>
 *   <li>{@code chat}: player, playerRef, message, author — {@code command}: player, playerRef, command</li>
 *   <li>others: player, detail, message</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerEvent(Long id, Instant ts, String type, String source,
                          String victim, String victimRef, String killer, String killerRef, String cause,
                          Boolean pvp, Boolean killerUnknown,
                          String actionId, String player, String playerRef, String admin, String reason,
                          String expiration, Boolean revoked,
                          String message, String kind, String loggedTime,
                          String session, List<String> identifiers,
                          String author, String command, String detail) {
    public ServerEvent {
        identifiers = Lists.of(identifiers);
    }
}
