package cloud.txapi.sdk.params;

/** Bucket size of {@code GET /events/activity}. */
public enum ActivityBucket {
    /** One bucket per hour (window at most 31 days, default). */
    HOUR("hour"),
    /** One bucket per day (window at most 366 days). */
    DAY("day");

    private final String value;

    ActivityBucket(String value) {
        this.value = value;
    }

    /** @return the wire value */
    public String value() {
        return value;
    }
}
