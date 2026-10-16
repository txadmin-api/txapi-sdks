package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * {@code GET /events/playtime}.
 *
 * @param success          always {@code true}
 * @param serverId         server
 * @param eventLog         event log enabled
 * @param since            start of the period
 * @param totalSeconds     play time of all players
 * @param sessionCount     sessions counted
 * @param unclosedSessions sessions without a leave older than 12 h (not counted); absent with {@code player}
 * @param players          ranking (or the one player)
 * @param recentSessions   with {@code player}: the player's last 50 sessions
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Playtime(boolean success, Long serverId, Boolean eventLog, Instant since, Long totalSeconds,
                       Long sessionCount, Integer unclosedSessions, List<Entry> players, List<Session> recentSessions) {
    public Playtime {
        players = Lists.of(players);
        recentSessions = Lists.of(recentSessions);
    }

    /**
     * @param player         player name
     * @param identifier     license, when known
     * @param sessions       sessions in the period
     * @param seconds        play time in the period
     * @param longestSeconds longest session
     * @param online         currently in an open session
     * @param lastSeen       last seen
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String player, String identifier, long sessions, long seconds, long longestSeconds,
                        boolean online, Instant lastSeen) {
    }

    /**
     * @param joined  join time
     * @param left    leave time, {@code null} while still online
     * @param seconds session length
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Session(Instant joined, Instant left, Long seconds) {
    }
}
