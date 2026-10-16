package cloud.txapi.sdk;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rate-limit state reported by the API with every response
 * ({@code X-RateLimit-Limit}, {@code X-RateLimit-Remaining}, {@code X-RateLimit-Reset}).
 *
 * <p>A component is {@code null} when the header was missing or not a number.
 *
 * @param limit        requests allowed in the current window
 * @param remaining    requests left in the current window
 * @param resetSeconds seconds until the window resets
 */
public record RateLimit(Long limit, Long remaining, Long resetSeconds) {

    /** Parses the rate-limit headers; {@code null} when none of them is present. */
    public static RateLimit fromHeaders(HttpHeaders headers) {
        return fromHeaders(headers.map());
    }

    /** Parses the rate-limit headers (case-insensitive names); {@code null} when none of them is present. */
    public static RateLimit fromHeaders(Map<String, List<String>> headers) {
        Long limit = number(headers, "x-ratelimit-limit");
        Long remaining = number(headers, "x-ratelimit-remaining");
        Long reset = number(headers, "x-ratelimit-reset");
        if (limit == null && remaining == null && reset == null) {
            return null;
        }
        return new RateLimit(limit, remaining, reset);
    }

    private static Long number(Map<String, List<String>> headers, String name) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).equals(name)
                    && !e.getValue().isEmpty()) {
                try {
                    return Long.parseLong(e.getValue().get(0).trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
