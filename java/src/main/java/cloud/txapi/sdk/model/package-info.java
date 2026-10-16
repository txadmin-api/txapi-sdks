/**
 * Typed responses of the txApi REST API.
 *
 * <p>All records ignore unknown JSON properties — the API adds fields in minor versions, and
 * that must never break an existing client. Fields the API leaves out are {@code null}
 * (boxed types) or an empty list/map. Timestamps that are ISO-8601 in the API are
 * {@link java.time.Instant}s; txAdmin's own Unix-second values stay {@code Long} with a
 * convenience accessor returning an {@code Instant}.
 */
package cloud.txapi.sdk.model;
