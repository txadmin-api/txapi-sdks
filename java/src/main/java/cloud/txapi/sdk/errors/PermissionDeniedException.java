package cloud.txapi.sdk.errors;

import cloud.txapi.sdk.RateLimit;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/** 403: the plan feature or key scope is missing, the caller's IP is not allowed, the server is disabled, or the txAdmin account lacks a permission. */
public class PermissionDeniedException extends ApiException {
    private static final long serialVersionUID = 1L;

    public PermissionDeniedException(int status, String message, JsonNode body, String rawBody,
            Map<String, List<String>> headers, RateLimit rateLimit) {
        super(status, message, body, rawBody, headers, rateLimit);
    }
}
