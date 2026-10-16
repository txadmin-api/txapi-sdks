package cloud.txapi.sdk.params;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Common parameters of the paged event-log lists ({@code GET /events}, {@code /events/deaths} …):
 * time window, page cursor and page size. Lists are newest first.
 *
 * <p>{@code since}/{@code until} take ISO-8601, epoch milliseconds or relative values
 * ({@code 30m}, {@code 24h}, {@code 7d}); the {@link Instant} and {@link Duration} overloads format
 * for you.
 *
 * @param <Q> the concrete query type
 */
public abstract class EventQueryParams<Q extends EventQueryParams<Q>> extends QueryParams<Q> {

    EventQueryParams(Map<String, Object> values) {
        super(values);
    }

    /** @param since start of the window as sent to the API ({@code 24h}, ISO-8601, epoch ms) @return a copy */
    public Q since(String since) {
        return with("since", since);
    }

    /** @param since start of the window @return a copy */
    public Q since(Instant since) {
        return with("since", time(since));
    }

    /** @param ago look-back window, e.g. {@code Duration.ofHours(24)} → {@code 24h} @return a copy */
    public Q since(Duration ago) {
        return with("since", relative(ago));
    }

    /** @return the window start as sent, or {@code null} */
    public String since() {
        return get("since");
    }

    /** @param until end of the window as sent to the API @return a copy */
    public Q until(String until) {
        return with("until", until);
    }

    /** @param until end of the window @return a copy */
    public Q until(Instant until) {
        return with("until", time(until));
    }

    /** @param ago end of the window as "this long ago" @return a copy */
    public Q until(Duration ago) {
        return with("until", relative(ago));
    }

    /** @return the window end as sent, or {@code null} */
    public String until() {
        return get("until");
    }

    /** @param cursor {@code nextCursor} of the previous page; {@code null} = first page @return a copy */
    public Q cursor(String cursor) {
        return with("cursor", cursor);
    }

    /** @return the page cursor, or {@code null} */
    public String cursor() {
        return get("cursor");
    }

    /** @param limit page size (API default 100) @return a copy */
    public Q limit(Integer limit) {
        return with("limit", limit);
    }

    /** @return the page size, or {@code null} */
    public Integer limit() {
        return get("limit");
    }
}
