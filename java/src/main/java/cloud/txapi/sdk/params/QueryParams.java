package cloud.txapi.sdk.params;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base class of the immutable query-parameter objects in this package.
 *
 * <p>Every setter-style method returns a <em>new</em> object, so an instance can be stored and
 * reused safely (also across threads):
 *
 * <pre>{@code
 * DeathQuery pvpToday = DeathQuery.create().pvp(true).since(Duration.ofHours(24));
 * }</pre>
 *
 * <p>Passing {@code null} to a setter removes the parameter again — absent parameters are never
 * sent (SPEC §2).
 *
 * @param <Q> the concrete type, returned by the setters
 */
public abstract class QueryParams<Q extends QueryParams<Q>> {
    private final Map<String, Object> values;

    QueryParams(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** Creates a copy of the concrete type holding {@code values}. */
    abstract Q make(Map<String, Object> values);

    final Q with(String name, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(values);
        if (value == null) {
            copy.remove(name);
        } else {
            copy.put(name, value);
        }
        return make(copy);
    }

    @SuppressWarnings("unchecked")
    final <T> T get(String name) {
        return (T) values.get(name);
    }

    /**
     * The query parameters this object sends, in insertion order, without absent values.
     * Values are {@link String}, {@link Long}, {@link Integer} or {@link Boolean}.
     *
     * @return an unmodifiable map
     */
    public Map<String, Object> toQuery() {
        return values;
    }

    /**
     * Formats a point in time the way the API's {@code since}/{@code until} parameters expect:
     * ISO-8601 ({@code 2026-10-01T00:00:00Z}).
     */
    static String time(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    /**
     * Formats a look-back window as a relative value the API understands ({@code 30m}, {@code 24h},
     * {@code 7d}). The API accepts minutes, hours and days; anything finer is rounded up to whole
     * minutes.
     */
    static String relative(Duration ago) {
        if (ago == null) {
            return null;
        }
        if (ago.isNegative() || ago.isZero()) {
            throw new IllegalArgumentException("duration must be positive: " + ago);
        }
        long seconds = ago.getSeconds() + (ago.getNano() > 0 ? 1 : 0);
        if (seconds % 86_400 == 0) {
            return (seconds / 86_400) + "d";
        }
        if (seconds % 3_600 == 0) {
            return (seconds / 3_600) + "h";
        }
        return ((seconds + 59) / 60) + "m";
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass() && values.equals(((QueryParams<?>) o).values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), values);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + values;
    }
}
