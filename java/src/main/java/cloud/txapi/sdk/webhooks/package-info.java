/**
 * Verifying incoming webhooks: {@link cloud.txapi.sdk.webhooks.Webhooks#verify(byte[], String, String)}
 * checks the {@code X-Txapi-Signature} header and returns the parsed
 * {@link cloud.txapi.sdk.webhooks.WebhookEvent}.
 *
 * <p><b>Always verify the raw request body bytes exactly as received.</b> Parsing the JSON and
 * serialising it again changes whitespace or key order and breaks the signature.
 */
package cloud.txapi.sdk.webhooks;
