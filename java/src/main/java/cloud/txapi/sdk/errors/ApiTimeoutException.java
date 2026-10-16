package cloud.txapi.sdk.errors;

/** No answer arrived within the configured timeout (per attempt), or a live command was not acknowledged in time. */
public class ApiTimeoutException extends ApiConnectionException {
    private static final long serialVersionUID = 1L;

    public ApiTimeoutException(String message) {
        super(message);
    }

    public ApiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
