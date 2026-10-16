package cloud.txapi.sdk.params;

import java.util.Map;

/**
 * Filters of {@code GET /events/sessions} — joins and leaves. Immutable; every setter returns a new object. Time window, cursor and page
 * size: see {@link EventQueryParams}.
 */
public final class SessionQuery extends EventQueryParams<SessionQuery> {

    private SessionQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (newest first, API default page size) */
    public static SessionQuery create() {
        return new SessionQuery(Map.of());
    }

    @Override
    SessionQuery make(Map<String, Object> values) {
        return new SessionQuery(values);
    }

    /** @param player player (name or identifier); {@code null} removes it @return a copy */
    public SessionQuery player(String player) {
        return with("player", player);
    }

    /** @return the player filter, or {@code null} */
    public String player() {
        return get("player");
    }
}
