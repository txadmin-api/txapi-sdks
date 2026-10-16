package cloud.txapi.sdk;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Asynchronous counterpart of {@link PageIterable}: pages are fetched one after another without
 * blocking a thread. Cancelling a returned future stops fetching further pages and cancels the
 * request in flight.
 *
 * <pre>{@code
 * client.async().players().searchAll(params)
 *       .forEach(p -> System.out.println(p.displayName()))
 *       .thenRun(() -> System.out.println("done"));
 * }</pre>
 *
 * @param <T> row type
 */
public interface AsyncPageIterable<T> {

    /**
     * Calls {@code action} for every row, page by page.
     *
     * @param action row callback; an exception from it fails the returned future and stops paging
     * @return completes when the last page was handled
     */
    CompletableFuture<Void> forEach(Consumer<? super T> action);

    /**
     * Calls {@code action} for every page (never an empty list).
     *
     * @param action page callback
     * @return completes when the last page was handled
     */
    CompletableFuture<Void> forEachPage(Consumer<? super List<T>> action);

    /**
     * Collects all rows.
     *
     * @return all rows
     */
    CompletableFuture<List<T>> toList();

    /**
     * Collects rows until {@code max} are reached; no further pages are fetched after that.
     *
     * @param max maximum number of rows (≥ 0)
     * @return at most {@code max} rows
     */
    CompletableFuture<List<T>> toList(int max);
}
