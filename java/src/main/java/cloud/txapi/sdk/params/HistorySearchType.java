package cloud.txapi.sdk.params;

/** What {@code search} is matched against in {@code GET /history/search} (default {@link #REASON}). */
public enum HistorySearchType {
    /** The action id (e.g. {@code ABCD-1234}). */
    ACTION_ID("actionId"),
    /** The reason text. */
    REASON("reason"),
    /** Player identifiers (license, discord, …). */
    IDENTIFIERS("identifiers");

    private final String value;

    HistorySearchType(String value) {
        this.value = value;
    }

    /** @return the wire value */
    public String value() {
        return value;
    }
}
