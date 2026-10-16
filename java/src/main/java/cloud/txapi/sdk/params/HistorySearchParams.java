package cloud.txapi.sdk.params;

import java.util.Map;

/**
 * Query of {@code GET /history/search}. Immutable; every setter returns a new object.
 *
 * <pre>{@code
 * HistorySearchParams p = HistorySearchParams.create()
 *         .type(HistoryType.BAN)
 *         .search("cheat");               // matched against the reason by default
 * }</pre>
 */
public final class HistorySearchParams extends QueryParams<HistorySearchParams> {

    private HistorySearchParams(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (bans and warnings, newest first) */
    public static HistorySearchParams create() {
        return new HistorySearchParams(Map.of());
    }

    @Override
    HistorySearchParams make(Map<String, Object> values) {
        return new HistorySearchParams(values);
    }

    /** @param type only bans or only warnings; {@code null} = both @return a copy */
    public HistorySearchParams type(HistoryType type) {
        return with("type", type == null ? null : type.value());
    }

    /** @return the type filter as sent, or {@code null} */
    public String type() {
        return get("type");
    }

    /** @param admin only actions of this txAdmin admin @return a copy */
    public HistorySearchParams admin(String admin) {
        return with("admin", admin);
    }

    /** @return the admin filter, or {@code null} */
    public String admin() {
        return get("admin");
    }

    /** @param searchType what {@link #search(String)} is matched against (default reason) @return a copy */
    public HistorySearchParams searchType(HistorySearchType searchType) {
        return with("searchType", searchType == null ? null : searchType.value());
    }

    /** @return the search type as sent, or {@code null} */
    public String searchType() {
        return get("searchType");
    }

    /** @param search search text @return a copy */
    public HistorySearchParams search(String search) {
        return with("search", search);
    }

    /** @return the search text, or {@code null} */
    public String search() {
        return get("search");
    }

    /** @param order sort direction by timestamp; {@code null} = API default ({@code desc}) @return a copy */
    public HistorySearchParams order(SortOrder order) {
        return with("order", order == null ? null : order.value());
    }

    /** @return the sort direction as sent, or {@code null} */
    public String order() {
        return get("order");
    }

    /** @param page position after the last row of the previous page; {@code null} = first @return a copy */
    public HistorySearchParams page(PageCursor page) {
        return with("offset", page == null ? null : page.offset()).with("cursor", page == null ? null : page.cursor());
    }

    /** @return the page position, or {@code null} */
    public PageCursor page() {
        Long offset = get("offset");
        String cursor = get("cursor");
        return offset == null || cursor == null ? null : new PageCursor(offset, cursor);
    }
}
