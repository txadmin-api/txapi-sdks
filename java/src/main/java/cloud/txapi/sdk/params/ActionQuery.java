package cloud.txapi.sdk.params;

import java.util.Map;

/**
 * Filters of {@code GET /events/actions} — bans and warnings from every source. Immutable; every setter returns a new object. Time window, cursor and page
 * size: see {@link EventQueryParams}.
 */
public final class ActionQuery extends EventQueryParams<ActionQuery> {

    private ActionQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (newest first, API default page size) */
    public static ActionQuery create() {
        return new ActionQuery(Map.of());
    }

    @Override
    ActionQuery make(Map<String, Object> values) {
        return new ActionQuery(values);
    }

    /** @param type {@code ban} or {@code warn}; {@code null} removes it @return a copy */
    public ActionQuery type(String type) {
        return with("type", type);
    }

    /** @return the type filter, or {@code null} */
    public String type() {
        return get("type");
    }

    /** @param player player (name or identifier); {@code null} removes it @return a copy */
    public ActionQuery player(String player) {
        return with("player", player);
    }

    /** @return the player filter, or {@code null} */
    public String player() {
        return get("player");
    }

    /** @param admin admin who acted; {@code null} removes it @return a copy */
    public ActionQuery admin(String admin) {
        return with("admin", admin);
    }

    /** @return the admin filter, or {@code null} */
    public String admin() {
        return get("admin");
    }

    /** @param revoked only revoked ({@code true}) or only active ({@code false}); {@code null} removes it @return a copy */
    public ActionQuery revoked(Boolean revoked) {
        return with("revoked", revoked);
    }

    /** @return the revoked filter, or {@code null} */
    public Boolean revoked() {
        return get("revoked");
    }
}
