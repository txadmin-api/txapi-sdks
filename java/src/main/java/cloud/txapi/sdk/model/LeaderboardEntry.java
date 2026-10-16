package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One leaderboard row.
 *
 * @param rank   1-based rank
 * @param player player name
 * @param kills  kills in the period
 * @param deaths deaths in the period
 * @param kd     kills per death
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeaderboardEntry(int rank, String player, long kills, long deaths, double kd) {
}
