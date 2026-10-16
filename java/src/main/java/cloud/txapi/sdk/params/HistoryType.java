package cloud.txapi.sdk.params;

/** Action type filter of {@code GET /history/search}. */
public enum HistoryType {
    /** Bans. */
    BAN("ban"),
    /** Warnings. */
    WARN("warn");

    private final String value;

    HistoryType(String value) {
        this.value = value;
    }

    /** @return the wire value */
    public String value() {
        return value;
    }
}
