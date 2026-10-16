package cloud.txapi.sdk.errors;

import cloud.txapi.sdk.RateLimit;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/** 422: txAdmin refused the action (for example because the player is offline). */
public class TxAdminRejectedException extends ApiException {
    private static final long serialVersionUID = 1L;

    public TxAdminRejectedException(int status, String message, JsonNode body, String rawBody,
            Map<String, List<String>> headers, RateLimit rateLimit) {
        super(status, message, body, rawBody, headers, rateLimit);
    }
}
