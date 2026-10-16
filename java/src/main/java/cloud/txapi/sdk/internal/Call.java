package cloud.txapi.sdk.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One API call before it is sent: endpoint, path/query/body values and the response type.
 * Absent ({@code null}) values are never recorded, so they are never sent.
 *
 * @param <T> response type
 */
public final class Call<T> {
    final Endpoint endpoint;
    final JavaType type;
    final Map<String, String> path = new LinkedHashMap<>();
    final Map<String, String> query = new LinkedHashMap<>();
    final Map<String, Object> body = new LinkedHashMap<>();

    private Call(Endpoint endpoint, JavaType type) {
        this.endpoint = endpoint;
        this.type = type;
    }

    public static <T> Call<T> of(Endpoint endpoint, Class<T> type) {
        return new Call<>(endpoint, Json.MAPPER.getTypeFactory().constructType(type));
    }

    public static <T> Call<T> of(Endpoint endpoint, TypeReference<T> type) {
        return new Call<>(endpoint, Json.MAPPER.getTypeFactory().constructType(type));
    }

    public Endpoint endpoint() {
        return endpoint;
    }

    public JavaType type() {
        return type;
    }

    public Call<T> path(String name, String value) {
        path.put(name, value);
        return this;
    }

    public Call<T> query(String name, Object value) {
        String s = format(value);
        if (s != null) {
            query.put(name, s);
        }
        return this;
    }

    public Call<T> query(Map<String, ?> values) {
        if (values != null) {
            values.forEach(this::query);
        }
        return this;
    }

    public Call<T> body(String name, Object value) {
        if (value != null) {
            body.put(name, value);
        }
        return this;
    }

    static String format(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b ? "true" : "false";
        }
        if (value instanceof Instant i) {
            return i.toString();
        }
        return String.valueOf(value);
    }
}
