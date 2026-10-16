package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * {@code GET /events/player}: statistics of one player. An unknown player gives
 * {@code found = false} (not a 404).
 *
 * @param success    always {@code true}
 * @param serverId   server
 * @param eventLog   event log enabled
 * @param player     player name
 * @param names      names seen for this player
 * @param identifier identifier, when the query was one
 * @param since      start of the period for kills/deaths/sessions
 * @param found      whether anything is known about the player
 * @param kills      kills in the period
 * @param deaths     deaths in the period
 * @param pvpDeaths  deaths by players in the period
 * @param kd         kills per death
 * @param topVictims most killed players
 * @param topKillers players who killed this one most often
 * @param topWeapons most used weapons
 * @param sessions   sessions in the period
 * @param firstSeen  first event ever stored
 * @param lastSeen   last event ever stored
 * @param bans       bans over the whole history
 * @param warns      warnings over the whole history
 * @param recent     the last 20 events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerEventStats(boolean success, Long serverId, Boolean eventLog, String player, List<String> names,
                               String identifier, Instant since, boolean found, Long kills, Long deaths,
                               Long pvpDeaths, Double kd, List<NameCount> topVictims, List<NameCount> topKillers,
                               List<NameCount> topWeapons, Long sessions, Instant firstSeen, Instant lastSeen,
                               Long bans, Long warns, List<ServerEvent> recent) {
    public PlayerEventStats {
        names = Lists.of(names);
        topVictims = Lists.of(topVictims);
        topKillers = Lists.of(topKillers);
        topWeapons = Lists.of(topWeapons);
        recent = Lists.of(recent);
    }
}
