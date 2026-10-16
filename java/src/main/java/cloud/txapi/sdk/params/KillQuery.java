package cloud.txapi.sdk.params;

import java.util.Map;

/**
 * Filters of {@code GET /events/kills} — player-vs-player kills. Immutable; every setter returns a new object. Time window, cursor and page
 * size: see {@link EventQueryParams}.
 */
public final class KillQuery extends EventQueryParams<KillQuery> {

    private KillQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (newest first, API default page size) */
    public static KillQuery create() {
        return new KillQuery(Map.of());
    }

    @Override
    KillQuery make(Map<String, Object> values) {
        return new KillQuery(values);
    }

    /** @param player player as killer or victim; {@code null} removes it @return a copy */
    public KillQuery player(String player) {
        return with("player", player);
    }

    /** @return the player filter, or {@code null} */
    public String player() {
        return get("player");
    }

    /** @param killer killer (name or identifier); {@code null} removes it @return a copy */
    public KillQuery killer(String killer) {
        return with("killer", killer);
    }

    /** @return the killer filter, or {@code null} */
    public String killer() {
        return get("killer");
    }

    /** @param victim victim (name or identifier); {@code null} removes it @return a copy */
    public KillQuery victim(String victim) {
        return with("victim", victim);
    }

    /** @return the victim filter, or {@code null} */
    public String victim() {
        return get("victim");
    }

    /** @param cause weapon / cause; {@code null} removes it @return a copy */
    public KillQuery cause(String cause) {
        return with("cause", cause);
    }

    /** @return the cause filter, or {@code null} */
    public String cause() {
        return get("cause");
    }
}
