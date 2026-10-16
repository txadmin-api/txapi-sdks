package cloud.txapi.sdk.errors;

/**
 * The request did not get an HTTP answer: DNS failure, connection refused or reset, TLS error,
 * or (for live streams) the socket is not connected.
 *
 * <p>GET requests are retried automatically before this is thrown; POST/PUT/DELETE are never
 * retried on transport errors, because a retried ban or restart could run twice.
 */
public class ApiConnectionException extends TxApiException {
    private static final long serialVersionUID = 1L;

    public ApiConnectionException(String message) {
        super(message);
    }

    public ApiConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
