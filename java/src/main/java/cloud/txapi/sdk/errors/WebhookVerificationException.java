package cloud.txapi.sdk.errors;

/**
 * A webhook delivery could not be verified: the signature header is missing or malformed,
 * no {@code v1} signature matches, the timestamp is outside the tolerance, or the body is not
 * a JSON event. {@link #getMessage()} names the reason. Answer such requests with 400.
 */
public class WebhookVerificationException extends TxApiException {
    private static final long serialVersionUID = 1L;

    public WebhookVerificationException(String message) {
        super(message);
    }

    public WebhookVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
