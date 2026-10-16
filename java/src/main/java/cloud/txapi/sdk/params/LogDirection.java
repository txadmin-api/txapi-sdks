package cloud.txapi.sdk.params;

/** Paging direction of {@code GET /server/log/history}. */
public enum LogDirection {
    /** Entries before {@code ref} (default). */
    OLDER("older"),
    /** Entries after {@code ref}. */
    NEWER("newer");

    private final String value;

    LogDirection(String value) {
        this.value = value;
    }

    /** @return the wire value */
    public String value() {
        return value;
    }
}
