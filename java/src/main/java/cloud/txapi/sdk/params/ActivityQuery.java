package cloud.txapi.sdk.params;

import java.util.Map;

/** Query of {@code GET /events/activity}. Immutable; every setter returns a new object. */
public final class ActivityQuery extends QueryParams<ActivityQuery> {

    private ActivityQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (API defaults) */
    public static ActivityQuery create() {
        return new ActivityQuery(Map.of());
    }

    @Override
    ActivityQuery make(Map<String, Object> values) {
        return new ActivityQuery(values);
    }

    /** @param since window start as sent ({@code 24h}, ISO-8601, epoch ms) @return a copy */
    public ActivityQuery since(String since) {
        return with("since", since);
    }

    /** @param since window start @return a copy */
    public ActivityQuery since(java.time.Instant since) {
        return with("since", time(since));
    }

    /** @param ago look-back window, e.g. {@code Duration.ofDays(7)} → {@code 7d} @return a copy */
    public ActivityQuery since(java.time.Duration ago) {
        return with("since", relative(ago));
    }

    /** @return the window start as sent, or {@code null} */
    public String since() {
        return get("since");
    }

    /** @param bucket bucket size (default hour) @return a copy */
    public ActivityQuery bucket(ActivityBucket bucket) {
        return with("bucket", bucket == null ? null : bucket.value());
    }

    /** @return the bucket size as sent, or {@code null} */
    public String bucket() {
        return get("bucket");
    }

    /** @param timeZone IANA zone for bucket boundaries, e.g. {@code Europe/Vienna} (default UTC) @return a copy */
    public ActivityQuery timeZone(String timeZone) {
        return with("timeZone", timeZone);
    }

    /** @param zone zone for bucket boundaries @return a copy */
    public ActivityQuery timeZone(java.time.ZoneId zone) {
        return with("timeZone", zone == null ? null : zone.getId());
    }

    /** @return the time zone as sent, or {@code null} */
    public String timeZone() {
        return get("timeZone");
    }
}
