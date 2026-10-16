package cloud.txapi.sdk;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Passed to the {@code onResponse} hook after every HTTP attempt that produced a response
 * (successful or not).
 *
 * @param request   the request this answers
 * @param status    HTTP status code
 * @param headers   response headers, names in lower case
 * @param rateLimit parsed rate-limit headers, or {@code null}
 * @param elapsed   time from sending to receiving the full body
 */
public record ResponseEvent(RequestEvent request, int status, Map<String, List<String>> headers,
                            RateLimit rateLimit, Duration elapsed) {
}
