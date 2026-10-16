/**
 * Exceptions. Everything the SDK throws extends the unchecked
 * {@link cloud.txapi.sdk.errors.TxApiException}; HTTP errors extend
 * {@link cloud.txapi.sdk.errors.ApiException} and carry status, body, headers and rate limit.
 *
 * <pre>
 * TxApiException
 * ├─ ApiException (any HTTP error)
 * │  ├─ BadRequestException          400 / 405 / 415
 * │  ├─ AuthenticationException      401
 * │  ├─ PermissionDeniedException    403
 * │  ├─ NotFoundException            404
 * │  ├─ ConflictException            409
 * │  ├─ TxAdminRejectedException     422
 * │  ├─ RateLimitException           429 (isQuotaExhausted(), retryAfter())
 * │  ├─ TxAdminUnavailableException  502
 * │  └─ ServerException              other 5xx
 * ├─ ApiConnectionException (network)
 * │  └─ ApiTimeoutException
 * ├─ RequestCancelledException
 * ├─ ValidationException (invalid input, before any network call)
 * ├─ CommandException (live console command failed)
 * └─ WebhookVerificationException
 * </pre>
 */
package cloud.txapi.sdk.errors;
