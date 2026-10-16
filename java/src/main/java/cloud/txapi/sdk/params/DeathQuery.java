package cloud.txapi.sdk.params;

import java.util.Map;

/**
 * Filters of {@code GET /events/deaths}. Immutable; every setter returns a new object. Time window, cursor and page
 * size: see {@link EventQueryParams}.
 */
public final class DeathQuery extends EventQueryParams<DeathQuery> {

    private DeathQuery(Map<String, Object> values) {
        super(values);
    }

    /** @return an empty query (newest first, API default page size) */
    public static DeathQuery create() {
        return new DeathQuery(Map.of());
    }

    @Override
    DeathQuery make(Map<String, Object> values) {
        return new DeathQuery(values);
    }

    /** @param player player as victim or killer (name or identifier); {@code null} removes it @return a copy */
    public DeathQuery player(String player) {
        return with("player", player);
    }

    /** @return the player filter, or {@code null} */
    public String player() {
        return get("player");
    }

    /** @param victim victim (name or identifier); {@code null} removes it @return a copy */
    public DeathQuery victim(String victim) {
        return with("victim", victim);
    }

    /** @return the victim filter, or {@code null} */
    public String victim() {
        return get("victim");
    }

    /** @param killer killer (name or identifier); {@code null} removes it @return a copy */
    public DeathQuery killer(String killer) {
        return with("killer", killer);
    }

    /** @return the killer filter, or {@code null} */
    public String killer() {
        return get("killer");
    }

    /** @param cause cause of death / weapon; {@code null} removes it @return a copy */
    public DeathQuery cause(String cause) {
        return with("cause", cause);
    }

    /** @return the cause filter, or {@code null} */
    public String cause() {
        return get("cause");
    }

    /** @param pvp only player-vs-player deaths ({@code true}) or only others ({@code false}); {@code null} removes it @return a copy */
    public DeathQuery pvp(Boolean pvp) {
        return with("pvp", pvp);
    }

    /** @return the pvp filter, or {@code null} */
    public Boolean pvp() {
        return get("pvp");
    }
}
