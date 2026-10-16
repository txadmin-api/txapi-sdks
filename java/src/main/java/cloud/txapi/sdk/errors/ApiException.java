package cloud.txapi.sdk.errors;

import cloud.txapi.sdk.RateLimit;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The API answered with an HTTP error status.
 *
 * <p>{@link #getMessage()} is the {@code error} text of the response body
 * ({@code {"success":false,"error":"…"}}), else the raw body, else the HTTP reason phrase.
 * Use the subclasses to react to specific situations, or {@link #status()} for anything else.
 */
public class ApiException extends TxApiException {
    private static final long serialVersionUID = 1L;

    private final int status;
    private final transient JsonNode body;
    private final String rawBody;
    private final Map<String, List<String>> headers;
    private final RateLimit rateLimit;

    public ApiException(int status, String message, JsonNode body, String rawBody,
                        Map<String, List<String>> headers, RateLimit rateLimit) {
        super(message);
        this.status = status;
        this.body = body;
        this.rawBody = rawBody == null ? "" : rawBody;
        this.headers = headers == null ? Map.of() : headers;
        this.rateLimit = rateLimit;
    }

    /** HTTP status code of the response. */
    public int status() {
        return status;
    }

    /** Parsed JSON body, or {@code null} when the body was not JSON (e.g. plain text from a proxy). */
    public JsonNode body() {
        return body;
    }

    /** The response body as text (empty string when there was none). */
    public String rawBody() {
        return rawBody;
    }

    /** Response headers (names in lower case). */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /** First value of a response header, case-insensitive. */
    public Optional<String> header(String name) {
        List<String> values = headers.get(name.toLowerCase(java.util.Locale.ROOT));
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }

    /** Rate-limit headers of this response, if the API sent them. */
    public Optional<RateLimit> rateLimit() {
        return Optional.ofNullable(rateLimit);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{status=" + status + ", message=" + getMessage() + "}";
    }
}
