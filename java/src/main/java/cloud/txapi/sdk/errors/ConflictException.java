package cloud.txapi.sdk.errors;

import cloud.txapi.sdk.RateLimit;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/** 409: the request conflicts with the account state, e.g. no server is configured. */
public class ConflictException extends ApiException {
    private static final long serialVersionUID = 1L;

    public ConflictException(int status, String message, JsonNode body, String rawBody,
            Map<String, List<String>> headers, RateLimit rateLimit) {
        super(status, message, body, rawBody, headers, rateLimit);
    }
}
