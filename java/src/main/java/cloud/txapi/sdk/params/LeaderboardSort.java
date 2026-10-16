package cloud.txapi.sdk.params;

/** Sort key of {@code GET /events/leaderboard}. */
public enum LeaderboardSort {
    /** Most kills (default). */
    KILLS("kills"),
    /** Most deaths. */
    DEATHS("deaths"),
    /** Best kill/death ratio. */
    KD("kd");

    private final String value;

    LeaderboardSort(String value) {
        this.value = value;
    }

    /** @return the wire value */
    public String value() {
        return value;
    }
}
