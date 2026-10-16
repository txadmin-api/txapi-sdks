package cloud.txapi.sdk.params;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Query of {@code GET /players/search}. Immutable; every setter returns a new object.
 *
 * <pre>{@code
 * PlayerSearchParams p = PlayerSearchParams.create()
 *         .query("john")
 *         .sortBy(PlayerSort.LAST_CONNECTION)
 *         .filters(PlayerFilter.ONLINE);
 * }</pre>
 */
public final class PlayerSearchParams extends QueryParams<PlayerSearchParams> {

    private PlayerSearchParams(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (all players, sorted by play time, newest first) */
    public static PlayerSearchParams create() {
        return new PlayerSearchParams(Map.of());
    }

    @Override
    PlayerSearchParams make(Map<String, Object> values) {
        return new PlayerSearchParams(values);
    }

    /** @param query name substring to search for; {@code null} = all players @return a copy */
    public PlayerSearchParams query(String query) {
        return with("query", query);
    }

    /** @return the search text, or {@code null} */
    public String query() {
        return get("query");
    }

    /** @param sortBy sort key; {@code null} = API default ({@code playTime}) @return a copy */
    public PlayerSearchParams sortBy(PlayerSort sortBy) {
        return with("sortBy", sortBy == null ? null : sortBy.value());
    }

    /**
     * @param sortBy wire value or alias: {@code playTime}, {@code tsJoined}, {@code tsLastConnection},
     *               {@code sessions}, {@code lastConnection}
     * @return a copy
     * @throws IllegalArgumentException for an unknown key
     */
    public PlayerSearchParams sortBy(String sortBy) {
        if (sortBy == null) {
            return with("sortBy", null);
        }
        PlayerSort.fromValue(sortBy); // fail early on unknown keys
        return with("sortBy", sortBy.strip());
    }

    /** @return the sort key as sent, or {@code null} */
    public String sortBy() {
        return get("sortBy");
    }

    /** @return the resolved sort key (aliases resolved; default {@link PlayerSort#PLAY_TIME}) */
    public PlayerSort sortKey() {
        return PlayerSort.fromValue(sortBy());
    }

    /** @param order sort direction; {@code null} = API default ({@code desc}) @return a copy */
    public PlayerSearchParams order(SortOrder order) {
        return with("order", order == null ? null : order.value());
    }

    /** @return the sort direction as sent ({@code asc}/{@code desc}), or {@code null} */
    public String order() {
        return get("order");
    }

    /** @param filters filters to combine (AND); none = no filter @return a copy */
    public PlayerSearchParams filters(PlayerFilter... filters) {
        if (filters == null || filters.length == 0) {
            return with("filters", null);
        }
        return with("filters", Arrays.stream(filters).filter(Objects::nonNull)
                .map(PlayerFilter::value).distinct().collect(Collectors.joining(",")));
    }

    /** @return the filter wire values, empty if none */
    public List<String> filters() {
        String f = get("filters");
        return f == null ? List.of() : List.of(f.split(","));
    }

    /**
     * Starts after the given row instead of at the beginning.
     *
     * @param page position, {@code null} = first page
     * @return a copy
     */
    public PlayerSearchParams page(PageCursor page) {
        PlayerSearchParams p = with("offset", page == null ? null : page.offset());
        return p.with("cursor", page == null ? null : page.cursor());
    }

    /** @return the page position, or {@code null} for the first page */
    public PageCursor page() {
        Long offset = get("offset");
        String cursor = get("cursor");
        return offset == null || cursor == null ? null : new PageCursor(offset, cursor);
    }

    /** @return a copy without the page position */
    public PlayerSearchParams firstPage() {
        Map<String, Object> m = new LinkedHashMap<>(toQuery());
        m.remove("offset");
        m.remove("cursor");
        return make(m);
    }
}
