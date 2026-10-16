package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * {@code GET /server/status}.
 *
 * @param serverName       server name in txAdmin
 * @param txaVersion       txAdmin version
 * @param fxsVersion       FXServer build
 * @param online           the FXServer process runs ({@code null} when txAdmin's diagnostics were unreadable)
 * @param txAdminReachable the txAdmin panel answers
 * @param playerStats      player database statistics (not the current player count)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerStatus(String serverName, String txaVersion, String fxsVersion, Boolean online,
                           Boolean txAdminReachable, PlayerStats playerStats) {

    /**
     * @param total         known players
     * @param playedLast24h players who played in the last 24 h
     * @param joinedLast24h new players in the last 24 h
     * @param joinedLast7d  new players in the last 7 days
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerStats(Long total, Long playedLast24h, Long joinedLast24h, Long joinedLast7d) {
    }
}
