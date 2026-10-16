package cloud.txapi.sdk.params;

/**
 * Position of a page in the player and history lists: {@code offset} is the sort-key value of the
 * last row of the previous page, {@code cursor} its {@code license} (players) or action {@code id}
 * (history). Normally you never build one yourself — use the {@code …All()} iterators.
 *
 * @param offset sort-key value of the last row (play time, join time, timestamp …)
 * @param cursor license (players) or action id (history) of the last row
 */
public record PageCursor(long offset, String cursor) {
    /**
     * @param offset sort-key value of the last row
     * @param cursor license / action id of the last row
     * @return the cursor
     */
    public static PageCursor of(long offset, String cursor) {
        return new PageCursor(offset, cursor);
    }
}
