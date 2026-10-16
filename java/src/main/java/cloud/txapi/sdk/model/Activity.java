package cloud.txapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * {@code GET /events/activity}: activity per hour or day (empty buckets are 0).
 *
 * @param success  always {@code true}
 * @param serverId server
 * @param eventLog event log enabled
 * @param since    start of the period
 * @param bucket   {@code hour} or {@code day}
 * @param timeZone time zone of the bucket boundaries
 * @param totals   sums over all buckets (without {@code players})
 * @param buckets  the buckets, oldest first
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Activity(boolean success, Long serverId, Boolean eventLog, Instant since, String bucket,
                       String timeZone, Totals totals, List<Bucket> buckets) {
    public Activity {
        buckets = Lists.of(buckets);
    }

    /** Sums over the whole period. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Totals(long joins, long deaths, long kills, long chat, long commands, long bans, long warns) {
    }

    /** One hour or day; {@code start} carries the offset of the requested time zone. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bucket(OffsetDateTime start, long joins, long players, long deaths, long kills, long chat,
                         long commands, long bans, long warns) {
    }
}
