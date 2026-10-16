package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * {@code GET /events/summary}.
 *
 * @param success       always {@code true}
 * @param serverId      server
 * @param eventLog      event log enabled
 * @param since         start of the period
 * @param byType        events per type in the period
 * @param kills         kills in the period
 * @param uniquePlayers distinct players in the period
 * @param topKiller     best killer of the period, {@code null} when nobody killed
 * @param topCauses     most frequent death causes
 * @param oldestEvent   oldest stored event
 * @param newestEvent   newest stored event
 * @param storedEvents  number of stored events
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventSummary(boolean success, Long serverId, Boolean eventLog, Instant since, Map<String, Long> byType,
                           Long kills, Long uniquePlayers, LeaderboardEntry topKiller, List<NameCount> topCauses,
                           Instant oldestEvent, Instant newestEvent, Long storedEvents) {
    public EventSummary {
        byType = Lists.of(byType);
        topCauses = Lists.of(topCauses);
    }
}
