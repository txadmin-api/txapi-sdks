package cloud.txapi.sdk.params;

/** Sort direction for player and history lists. The API default is {@link #DESC}. */
public enum SortOrder {
    /** Smallest value first. */
    ASC("asc"),
    /** Largest value first (API default). */
    DESC("desc");

    private final String value;

    SortOrder(String value) {
        this.value = value;
    }

    /** @return the wire value ({@code asc} / {@code desc}) */
    public String value() {
        return value;
    }
}
