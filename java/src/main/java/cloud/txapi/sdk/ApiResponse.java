package cloud.txapi.sdk;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parsed data together with the HTTP response it came from.
 * Obtain one with {@link TxApi#withRawResponse(java.util.function.Function)} or
 * {@link TxApiAsync#withRawResponse(java.util.function.Function)}.
 *
 * @param <T> type of the parsed data
 */
public final class ApiResponse<T> {
    private final int status;
    private final Map<String, List<String>> headers;
    private final RateLimit rateLimit;
    private final T data;

    public ApiResponse(int status, Map<String, List<String>> headers, RateLimit rateLimit, T data) {
        this.status = status;
        this.headers = headers == null ? Map.of() : headers;
        this.rateLimit = rateLimit;
        this.data = data;
    }

    /** HTTP status code (2xx). */
    public int status() {
        return status;
    }

    /** Response headers, names in lower case. */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /** First value of a header, case-insensitive. */
    public Optional<String> header(String name) {
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    /** Rate-limit headers of this response, if present. */
    public Optional<RateLimit> rateLimit() {
        return Optional.ofNullable(rateLimit);
    }

    /** The parsed body (the same value the plain method returns). */
    public T data() {
        return data;
    }

    @Override
    public String toString() {
        return "ApiResponse{status=" + status + ", rateLimit=" + rateLimit + ", data=" + data + "}";
    }
}
