package cloud.txapi.sdk.params;

import java.util.Map;

/**
 * Filters of {@code GET /events/admin-actions} — txAdmin panel and menu actions. Immutable; every setter returns a new object. Time window, cursor and page
 * size: see {@link EventQueryParams}.
 */
public final class AdminActionQuery extends EventQueryParams<AdminActionQuery> {

    private AdminActionQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (newest first, API default page size) */
    public static AdminActionQuery create() {
        return new AdminActionQuery(Map.of());
    }

    @Override
    AdminActionQuery make(Map<String, Object> values) {
        return new AdminActionQuery(values);
    }

    /** @param admin txAdmin admin name; {@code null} removes it @return a copy */
    public AdminActionQuery admin(String admin) {
        return with("admin", admin);
    }

    /** @return the admin filter, or {@code null} */
    public String admin() {
        return get("admin");
    }

    /** @param kind action kind, e.g. {@code ban}, {@code kick}, {@code command}; {@code null} removes it @return a copy */
    public AdminActionQuery kind(String kind) {
        return with("kind", kind);
    }

    /** @return the kind filter, or {@code null} */
    public String kind() {
        return get("kind");
    }

    /** @param search text search; {@code null} removes it @return a copy */
    public AdminActionQuery search(String search) {
        return with("search", search);
    }

    /** @return the search filter, or {@code null} */
    public String search() {
        return get("search");
    }
}
