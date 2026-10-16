package cloud.txapi.sdk.params;

import java.util.Map;

/** Query of {@code GET /events/playtime}. Immutable; every setter returns a new object. */
public final class PlaytimeQuery extends QueryParams<PlaytimeQuery> {

    private PlaytimeQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (API defaults) */
    public static PlaytimeQuery create() {
        return new PlaytimeQuery(Map.of());
    }

    @Override
    PlaytimeQuery make(Map<String, Object> values) {
        return new PlaytimeQuery(values);
    }

    /** @param since window start as sent ({@code 24h}, ISO-8601, epoch ms) @return a copy */
    public PlaytimeQuery since(String since) {
        return with("since", since);
    }

    /** @param since window start @return a copy */
    public PlaytimeQuery since(java.time.Instant since) {
        return with("since", time(since));
    }

    /** @param ago look-back window, e.g. {@code Duration.ofDays(7)} → {@code 7d} @return a copy */
    public PlaytimeQuery since(java.time.Duration ago) {
        return with("since", relative(ago));
    }

    /** @return the window start as sent, or {@code null} */
    public String since() {
        return get("since");
    }

    /** @param player only this player (name or identifier); adds their recent sessions @return a copy */
    public PlaytimeQuery player(String player) {
        return with("player", player);
    }

    /** @return the player filter, or {@code null} */
    public String player() {
        return get("player");
    }

    /** @param limit number of players in the ranking @return a copy */
    public PlaytimeQuery limit(Integer limit) {
        return with("limit", limit);
    }

    /** @return the limit, or {@code null} */
    public Integer limit() {
        return get("limit");
    }
}
