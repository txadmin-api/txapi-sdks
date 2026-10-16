package cloud.txapi.sdk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Lazily paged list: pages are fetched only while you iterate (SPEC §7). Each iteration starts at
 * the first page again.
 *
 * <pre>{@code
 * for (Player p : client.players().searchAll(PlayerSearchParams.create().query("john"))) { … }
 *
 * List<String> banned = client.history().bansAll().stream()
 *         .limit(500)                           // stops fetching after 500 rows
 *         .map(HistoryEntry::playerName)
 *         .toList();
 * }</pre>
 *
 * <p>Iteration stops when the API reports the end ({@code hasReachedEnd}, no {@code nextCursor}),
 * on an empty page, or when a page would repeat. Errors are thrown from {@link Iterator#hasNext()}
 * / {@link Iterator#next()} as the usual {@link cloud.txapi.sdk.errors.TxApiException}s; an
 * interrupted thread stops with {@link cloud.txapi.sdk.errors.RequestCancelledException}.
 *
 * @param <T> row type
 */
public interface PageIterable<T> extends Iterable<T> {

    /**
     * Iterates page by page instead of row by row.
     *
     * @return a lazy iterable of pages (never empty lists)
     */
    Iterable<List<T>> pages();

    @Override
    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED | Spliterator.NONNULL);
    }

    /**
     * Sequential, lazy stream over all rows; short-circuiting operations ({@code limit},
     * {@code findFirst}, {@code anyMatch}) stop fetching pages.
     *
     * @return a stream of rows
     */
    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Fetches every page and collects all rows. Prefer {@link #stream()} with a limit for big lists.
     *
     * @return all rows
     */
    default List<T> toList() {
        List<T> out = new ArrayList<>();
        forEach(out::add);
        return out;
    }
}
