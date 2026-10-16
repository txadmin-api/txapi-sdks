package cloud.txapi.sdk.params;

import java.util.Map;

/**
 * Filters of {@code GET /events/chat} (needs chat logging switched on for the server). Immutable; every setter returns a new object. Time window, cursor and page
 * size: see {@link EventQueryParams}.
 */
public final class ChatQuery extends EventQueryParams<ChatQuery> {

    private ChatQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (newest first, API default page size) */
    public static ChatQuery create() {
        return new ChatQuery(Map.of());
    }

    @Override
    ChatQuery make(Map<String, Object> values) {
        return new ChatQuery(values);
    }

    /** @param player author (name or identifier); {@code null} removes it @return a copy */
    public ChatQuery player(String player) {
        return with("player", player);
    }

    /** @return the player filter, or {@code null} */
    public String player() {
        return get("player");
    }

    /** @param search text search; {@code null} removes it @return a copy */
    public ChatQuery search(String search) {
        return with("search", search);
    }

    /** @return the search filter, or {@code null} */
    public String search() {
        return get("search");
    }
}
