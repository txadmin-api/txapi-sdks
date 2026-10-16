package cloud.txapi.sdk.params;

import java.util.Map;

/**
 * Query of {@code GET /history/bans} and {@code GET /history/warns}: sort direction and page
 * position. Immutable; every setter returns a new object.
 */
public final class HistoryPageParams extends QueryParams<HistoryPageParams> {

    private HistoryPageParams(Map<String, Object> values) {
        super(values);
    }

    /** @return defaults: newest first, first page */
    public static HistoryPageParams create() {
        return new HistoryPageParams(Map.of());
    }

    @Override
    HistoryPageParams make(Map<String, Object> values) {
        return new HistoryPageParams(values);
    }

    /** @param order sort direction by timestamp; {@code null} = API default ({@code desc}) @return a copy */
    public HistoryPageParams order(SortOrder order) {
        return with("order", order == null ? null : order.value());
    }

    /** @return the sort direction as sent, or {@code null} */
    public String order() {
        return get("order");
    }

    /** @param page position after the last row of the previous page; {@code null} = first @return a copy */
    public HistoryPageParams page(PageCursor page) {
        return with("offset", page == null ? null : page.offset()).with("cursor", page == null ? null : page.cursor());
    }

    /** @return the page position, or {@code null} */
    public PageCursor page() {
        Long offset = get("offset");
        String cursor = get("cursor");
        return offset == null || cursor == null ? null : new PageCursor(offset, cursor);
    }
}
