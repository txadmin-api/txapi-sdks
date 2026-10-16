package cloud.txapi.sdk.errors;

import cloud.txapi.sdk.RateLimit;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/** 502: txAdmin was unreachable or answered with something unusable. */
public class TxAdminUnavailableException extends ApiException {
    private static final long serialVersionUID = 1L;

    public TxAdminUnavailableException(int status, String message, JsonNode body, String rawBody,
            Map<String, List<String>> headers, RateLimit rateLimit) {
        super(status, message, body, rawBody, headers, rateLimit);
    }
}
