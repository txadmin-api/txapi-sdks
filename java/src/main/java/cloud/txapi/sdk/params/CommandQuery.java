package cloud.txapi.sdk.params;

import java.util.Map;

/**
 * Filters of {@code GET /events/commands}. Immutable; every setter returns a new object. Time window, cursor and page
 * size: see {@link EventQueryParams}.
 */
public final class CommandQuery extends EventQueryParams<CommandQuery> {

    private CommandQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (newest first, API default page size) */
    public static CommandQuery create() {
        return new CommandQuery(Map.of());
    }

    @Override
    CommandQuery make(Map<String, Object> values) {
        return new CommandQuery(values);
    }

    /** @param player player who ran the command; {@code null} removes it @return a copy */
    public CommandQuery player(String player) {
        return with("player", player);
    }

    /** @return the player filter, or {@code null} */
    public String player() {
        return get("player");
    }

    /** @param search text search; {@code null} removes it @return a copy */
    public CommandQuery search(String search) {
        return with("search", search);
    }

    /** @return the search filter, or {@code null} */
    public String search() {
        return get("search");
    }
}
