package cloud.txapi.sdk.errors;

/**
 * The calling thread was interrupted while the SDK was sending a request or waiting to retry.
 * The thread's interrupt flag is set again before this is thrown.
 */
public class RequestCancelledException extends TxApiException {
    private static final long serialVersionUID = 1L;

    public RequestCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}
