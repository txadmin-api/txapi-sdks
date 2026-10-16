package cloud.txapi.sdk.errors;

/**
 * Base class of every exception thrown by the txApi SDK.
 *
 * <p>All SDK exceptions are unchecked. The hierarchy is:
 * <pre>
 * TxApiException
 * ├── ApiException                 the API answered with an error status (4xx/5xx)
 * │   ├── BadRequestException      400, 405, 415
 * │   ├── AuthenticationException  401
 * │   ├── PermissionDeniedException 403
 * │   ├── NotFoundException        404
 * │   ├── ConflictException        409
 * │   ├── TxAdminRejectedException 422
 * │   ├── RateLimitException       429
 * │   ├── TxAdminUnavailableException 502
 * │   └── ServerException          other 5xx
 * ├── ApiConnectionException       transport problem (DNS, refused, reset …)
 * │   └── ApiTimeoutException      no answer within the timeout
 * ├── RequestCancelledException    the calling thread was interrupted
 * ├── ValidationException          client-side check failed, nothing was sent
 * ├── CommandException             the live console rejected a command
 * └── WebhookVerificationException a webhook signature did not verify
 * </pre>
 *
 * <p>No exception of this SDK ever contains the API key in its message or {@code toString()}.
 */
public class TxApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TxApiException(String message) {
        super(message);
    }

    public TxApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
