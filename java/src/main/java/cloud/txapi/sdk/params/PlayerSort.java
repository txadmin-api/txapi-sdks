package cloud.txapi.sdk.params;

/**
 * Sort keys of {@code GET /players/search}. The value of the sort key of the last row is also the
 * pagination {@code offset} (SPEC §7).
 */
public enum PlayerSort {
    /** Total play time (API default). */
    PLAY_TIME("playTime"),
    /** First join ({@code tsJoined}); the API also accepts the alias {@code sessions}. */
    JOINED("tsJoined"),
    /** Last connection ({@code tsLastConnection}); alias {@code lastConnection}. */
    LAST_CONNECTION("tsLastConnection");

    private final String value;

    PlayerSort(String value) {
        this.value = value;
    }

    /** @return the wire value ({@code playTime}, {@code tsJoined}, {@code tsLastConnection}) */
    public String value() {
        return value;
    }

    /**
     * Resolves a wire value or alias ({@code sessions} → {@link #JOINED}, {@code lastConnection} →
     * {@link #LAST_CONNECTION}); {@code null}/blank → {@link #PLAY_TIME}.
     *
     * @param value wire value or alias
     * @return the sort key
     * @throws IllegalArgumentException for an unknown value (the API answers 400 for it)
     */
    public static PlayerSort fromValue(String value) {
        if (value == null || value.isBlank()) {
            return PLAY_TIME;
        }
        return switch (value.strip()) {
            case "playTime" -> PLAY_TIME;
            case "tsJoined", "sessions" -> JOINED;
            case "tsLastConnection", "lastConnection" -> LAST_CONNECTION;
            default -> throw new IllegalArgumentException("unknown player sort key '" + value
                    + "' (allowed: playTime, tsJoined, tsLastConnection, sessions, lastConnection)");
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
