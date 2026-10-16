package cloud.txapi.sdk.errors;

/**
 * A client-side check failed and <b>no request was sent</b>: a required field is missing or
 * blank, a text is longer than the API accepts, or the client is misconfigured (no API key).
 *
 * <p>The SDK checks only what the API documents (required fields, {@code maxLength});
 * the server stays the authority for everything else.
 */
public class ValidationException extends TxApiException {
    private static final long serialVersionUID = 1L;

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /** Name of the offending field or option, or {@code null}. */
    public String field() {
        return field;
    }
}
