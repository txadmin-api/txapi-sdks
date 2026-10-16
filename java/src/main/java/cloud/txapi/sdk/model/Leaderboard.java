package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * {@code GET /events/leaderboard}.
 *
 * @param success  always {@code true}
 * @param serverId server
 * @param eventLog event log enabled
 * @param since    start of the period
 * @param sort     {@code kills}, {@code deaths} or {@code kd}
 * @param players  the ranking
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Leaderboard(boolean success, Long serverId, Boolean eventLog, Instant since, String sort,
                          List<LeaderboardEntry> players) {
    public Leaderboard {
        players = Lists.of(players);
    }
}
