package cloud.txapi.sdk.params;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Filters of {@code GET /events} — every stored event. Immutable; every setter returns a new object. Time window, cursor and page
 * size: see {@link EventQueryParams}.
 */
public final class EventListQuery extends EventQueryParams<EventListQuery> {

    private EventListQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (newest first, API default page size) */
    public static EventListQuery create() {
        return new EventListQuery(Map.of());
    }

    @Override
    EventListQuery make(Map<String, Object> values) {
        return new EventListQuery(values);
    }

    /**
     * @param type event type, e.g. {@code death}, {@code join}, {@code ban} — or several, comma-separated;
     *             {@code null} removes it
     * @return a copy
     */
    public EventListQuery type(String type) {
        return with("type", type);
    }

    /**
     * Several event types at once ({@code type("death", "ban")}); sent comma-separated.
     *
     * @param types event types; none removes the filter
     * @return a copy
     */
    public EventListQuery type(String... types) {
        if (types == null || types.length == 0) {
            return with("type", null);
        }
        String joined = Arrays.stream(types).filter(Objects::nonNull).map(String::strip)
                .filter(t -> !t.isEmpty()).distinct().collect(Collectors.joining(","));
        return with("type", joined.isEmpty() ? null : joined);
    }

    /** @return the type filter, or {@code null} */
    public String type() {
        return get("type");
    }

    /** @param player player name (exact, case-insensitive) or identifier such as {@code license:…}; {@code null} removes it @return a copy */
    public EventListQuery player(String player) {
        return with("player", player);
    }

    /** @return the player filter, or {@code null} */
    public String player() {
        return get("player");
    }

    /** @param search text search; {@code null} removes it @return a copy */
    public EventListQuery search(String search) {
        return with("search", search);
    }

    /** @return the search filter, or {@code null} */
    public String search() {
        return get("search");
    }
}
