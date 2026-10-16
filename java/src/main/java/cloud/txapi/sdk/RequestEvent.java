package cloud.txapi.sdk;

import java.net.URI;

/**
 * Passed to the {@code onRequest} hook before every HTTP attempt (also before each retry).
 * The URI never contains the API key (it travels in the {@code Authorization} header).
 *
 * @param endpointId catalog id of the endpoint, e.g. {@code players.ban}
 * @param method     HTTP method
 * @param uri        full request URI including the query string
 * @param attempt    0 for the first attempt, 1 for the first retry …
 */
public record RequestEvent(String endpointId, String method, URI uri, int attempt) {
}
