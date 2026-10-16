package cloud.txapi.sdk.errors;

import cloud.txapi.sdk.RateLimit;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 429 Too Many Requests.
 *
 * <p>Two different situations share this status:
 * <ul>
 *   <li><b>Per-minute limit</b> — the response carries {@code Retry-After}; {@link #retryAfter()}
 *       says how long to wait. The SDK already retried this automatically (up to 60 s) before
 *       throwing.</li>
 *   <li><b>Monthly quota exhausted</b> — no {@code Retry-After}; {@link #isQuotaExhausted()} is
 *       {@code true}. Waiting does not help: the quota resets on the 1st of the month, 00:00 UTC.</li>
 * </ul>
 */
public class RateLimitException extends ApiException {
    private static final long serialVersionUID = 1L;

    private final Duration retryAfter;
    private final boolean quotaExhausted;

    public RateLimitException(int status, String message, JsonNode body, String rawBody,
                              Map<String, List<String>> headers, RateLimit rateLimit,
                              Duration retryAfter, boolean quotaExhausted) {
        super(status, message, body, rawBody, headers, rateLimit);
        this.retryAfter = retryAfter;
        this.quotaExhausted = quotaExhausted;
    }

    /** How long the API asked to wait ({@code Retry-After}), if it said so. */
    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }

    /** {@code true} when the monthly quota is used up (429 without {@code Retry-After}). */
    public boolean isQuotaExhausted() {
        return quotaExhausted;
    }

    @Override
    public String toString() {
        return "RateLimitException{status=" + status() + ", message=" + getMessage()
                + ", retryAfter=" + retryAfter + ", quotaExhausted=" + quotaExhausted + "}";
    }
}
