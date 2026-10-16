package cloud.txapi.sdk.internal;

import cloud.txapi.sdk.AsyncPageIterable;
import cloud.txapi.sdk.PageIterable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Lazy pagination (SPEC §7). Internal API. */
public final class Pager {
    private Pager() {
    }

    /**
     * One fetched page.
     *
     * @param items rows (may be empty)
     * @param next  token of the next page, {@code null} = this was the last page
     */
    public record Page<T>(List<T> items, Object next) {
        public Page {
            items = items == null ? List.of() : items;
        }
    }

    /** Fetches the page for a token ({@code null} = first page). */
    public interface Fetch<T> {
        Page<T> fetch(Object token);
    }

    /** Asynchronously fetches the page for a token ({@code null} = first page). */
    public interface AsyncFetch<T> {
        CompletableFuture<Page<T>> fetch(Object token);
    }

    static boolean last(Page<?> page, Object token) {
        return page.items().isEmpty() || page.next() == null || Objects.equals(page.next(), token);
    }

    public static <T> PageIterable<T> sync(Fetch<T> fetch) {
        return new SyncPages<>(fetch);
    }

    public static <T> AsyncPageIterable<T> async(AsyncFetch<T> fetch) {
        return new AsyncPages<>(fetch);
    }

    private record SyncPages<T>(Fetch<T> fetch) implements PageIterable<T> {
        @Override
        public Iterable<List<T>> pages() {
            return PageIterator::new;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                private final PageIterator pages = new PageIterator();
                private Iterator<T> current = List.<T>of().iterator();

                @Override
                public boolean hasNext() {
                    while (!current.hasNext()) {
                        if (!pages.hasNext()) {
                            return false;
                        }
                        current = pages.next().iterator();
                    }
                    return true;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return current.next();
                }
            };
        }

        @Override
        public String toString() {
            return "PageIterable";
        }

        private final class PageIterator implements Iterator<List<T>> {
            private Object token;
            private boolean started;
            private boolean done;
            private List<T> buffered;

            @Override
            public boolean hasNext() {
                if (buffered != null) {
                    return true;
                }
                if (done) {
                    return false;
                }
                Object requested = started ? token : null;
                Page<T> page = fetch.fetch(requested);
                started = true;
                if (last(page, requested)) {
                    done = true;
                } else {
                    token = page.next();
                }
                if (page.items().isEmpty()) {
                    return false;
                }
                buffered = page.items();
                return true;
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                List<T> out = buffered;
                buffered = null;
                return out;
            }
        }
    }

    private record AsyncPages<T>(AsyncFetch<T> fetch) implements AsyncPageIterable<T> {
        @Override
        public CompletableFuture<Void> forEach(Consumer<? super T> action) {
            Objects.requireNonNull(action, "action");
            return run(page -> {
                page.forEach(action);
                return true;
            });
        }

        @Override
        public CompletableFuture<Void> forEachPage(Consumer<? super List<T>> action) {
            Objects.requireNonNull(action, "action");
            return run(page -> {
                action.accept(page);
                return true;
            });
        }

        @Override
        public CompletableFuture<List<T>> toList() {
            List<T> out = new ArrayList<>();
            return run(page -> {
                out.addAll(page);
                return true;
            }).thenApply(v -> out);
        }

        @Override
        public CompletableFuture<List<T>> toList(int max) {
            if (max < 0) {
                throw new IllegalArgumentException("max must be >= 0");
            }
            List<T> out = new ArrayList<>();
            if (max == 0) {
                return CompletableFuture.completedFuture(out);
            }
            return run(page -> {
                for (T t : page) {
                    out.add(t);
                    if (out.size() >= max) {
                        return false;
                    }
                }
                return true;
            }).thenApply(v -> out);
        }

        /** @param onPage returns {@code false} to stop */
        private CompletableFuture<Void> run(Function<List<T>, Boolean> onPage) {
            CompletableFuture<Void> done = new CompletableFuture<>();
            step(null, done, onPage::apply);
            return done;
        }

        private void step(Object token, CompletableFuture<Void> done, Predicate<List<T>> onPage) {
            if (done.isDone()) {
                return;
            }
            CompletableFuture<Page<T>> inFlight;
            try {
                inFlight = fetch.fetch(token);
            } catch (RuntimeException e) {
                done.completeExceptionally(e);
                return;
            }
            done.whenComplete((v, t) -> {
                if (done.isCancelled()) {
                    inFlight.cancel(true);
                }
            });
            inFlight.whenComplete((page, error) -> {
                if (done.isDone()) {
                    return;
                }
                if (error != null) {
                    Throwable cause = ClientCore.unwrap(error);
                    if (cause instanceof CancellationException) {
                        done.cancel(true);
                    } else {
                        done.completeExceptionally(cause);
                    }
                    return;
                }
                boolean more;
                try {
                    more = page.items().isEmpty() || onPage.test(page.items());
                } catch (Throwable t) {
                    done.completeExceptionally(t);
                    return;
                }
                if (!more || last(page, token)) {
                    done.complete(null);
                } else {
                    step(page.next(), done, onPage);
                }
            });
        }
    }
}
