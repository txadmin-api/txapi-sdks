package cloud.txapi.sdk.params;

/** Filters of {@code GET /players/search} (txAdmin's simple filters, combined with AND). */
public enum PlayerFilter {
    /** Connected right now. */
    ONLINE("isOnline"),
    /** Has txAdmin admin rights. */
    ADMIN("isAdmin"),
    /** On the whitelist. */
    WHITELISTED("isWhitelisted"),
    /** Has a note. */
    HAS_NOTE("hasNote");

    private final String value;

    PlayerFilter(String value) {
        this.value = value;
    }

    /** @return the wire value, e.g. {@code isOnline} */
    public String value() {
        return value;
    }
}
