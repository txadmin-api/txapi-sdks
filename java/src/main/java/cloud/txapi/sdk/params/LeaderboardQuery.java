package cloud.txapi.sdk.params;

import java.util.Map;

/** Query of {@code GET /events/leaderboard}. Immutable; every setter returns a new object. */
public final class LeaderboardQuery extends QueryParams<LeaderboardQuery> {

    private LeaderboardQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (API defaults) */
    public static LeaderboardQuery create() {
        return new LeaderboardQuery(Map.of());
    }

    @Override
    LeaderboardQuery make(Map<String, Object> values) {
        return new LeaderboardQuery(values);
    }

    /** @param since window start as sent ({@code 24h}, ISO-8601, epoch ms) @return a copy */
    public LeaderboardQuery since(String since) {
        return with("since", since);
    }

    /** @param since window start @return a copy */
    public LeaderboardQuery since(java.time.Instant since) {
        return with("since", time(since));
    }

    /** @param ago look-back window, e.g. {@code Duration.ofDays(7)} → {@code 7d} @return a copy */
    public LeaderboardQuery since(java.time.Duration ago) {
        return with("since", relative(ago));
    }

    /** @return the window start as sent, or {@code null} */
    public String since() {
        return get("since");
    }

    /** @param sort sort key (default kills) @return a copy */
    public LeaderboardQuery sort(LeaderboardSort sort) {
        return with("sort", sort == null ? null : sort.value());
    }

    /** @return the sort key as sent, or {@code null} */
    public String sort() {
        return get("sort");
    }

    /** @param limit number of players @return a copy */
    public LeaderboardQuery limit(Integer limit) {
        return with("limit", limit);
    }

    /** @return the limit, or {@code null} */
    public Integer limit() {
        return get("limit");
    }
}
