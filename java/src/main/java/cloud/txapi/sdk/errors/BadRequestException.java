package cloud.txapi.sdk.errors;

import cloud.txapi.sdk.RateLimit;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/** 400, 405 or 415: the request was invalid (missing or malformed parameter, wrong method or content type). */
public class BadRequestException extends ApiException {
    private static final long serialVersionUID = 1L;

    public BadRequestException(int status, String message, JsonNode body, String rawBody,
            Map<String, List<String>> headers, RateLimit rateLimit) {
        super(status, message, body, rawBody, headers, rateLimit);
    }
}
