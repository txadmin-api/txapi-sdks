package cloud.txapi.sdk.resources;

import cloud.txapi.sdk.AsyncPageIterable;
import cloud.txapi.sdk.PageIterable;
import cloud.txapi.sdk.RequestOptions;
import cloud.txapi.sdk.internal.Call;
import cloud.txapi.sdk.internal.Endpoint;
import cloud.txapi.sdk.internal.Invoker;
import cloud.txapi.sdk.internal.Pager;
import cloud.txapi.sdk.internal.Tokens;
import cloud.txapi.sdk.live.*;
import cloud.txapi.sdk.model.*;
import cloud.txapi.sdk.params.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Whitelist requests and pre-approvals ({@code /whitelist/…}).
 *
 * <p>Obtain it from {@code client.whitelist()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncWhitelist
 */
@SuppressWarnings("unused")
public final class Whitelist {
    private final Invoker inv;
    private final WhitelistApprovals approvals;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public Whitelist(Invoker invoker) {
        this.inv = invoker;
        this.approvals = new WhitelistApprovals(invoker);
    }

    /**
     * Pending whitelist requests (first page), passed through from txAdmin.
     *
     * <p>{@code GET /api/v1/whitelist/requests} · permission {@code players.read}
     *
     * @return the requests (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode requests() {
        return inv.call(Calls.whitelist_req0(), null);
    }

    /**
     * Pending whitelist requests (first page), passed through from txAdmin.
     *
     * <p>{@code GET /api/v1/whitelist/requests} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the requests (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode requests(RequestOptions options) {
        return inv.call(Calls.whitelist_req0(), options);
    }

    /**
     * Pending whitelist requests, one page, passed through from txAdmin.
     *
     * <p>{@code GET /api/v1/whitelist/requests} · permission {@code players.read}
     *
     * @param page page number (txAdmin's paging); {@code null} = first
     * @return the requests (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode requests(Integer page) {
        return inv.call(Calls.whitelist_req1(page), null);
    }

    /**
     * Pending whitelist requests, one page, passed through from txAdmin.
     *
     * <p>{@code GET /api/v1/whitelist/requests} · permission {@code players.read}
     *
     * @param page page number (txAdmin's paging); {@code null} = first
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the requests (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode requests(Integer page, RequestOptions options) {
        return inv.call(Calls.whitelist_req1(page), options);
    }

    /**
     * Approves a whitelist request.
     *
     * <p>{@code POST /api/v1/whitelist/requests/{requestId}/approve} · permission {@code players.write}
     *
     * @param requestId request id, e.g. {@code R1234}
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult approve(String requestId) {
        return inv.call(Calls.whitelist_approve(requestId), null);
    }

    /**
     * Approves a whitelist request.
     *
     * <p>{@code POST /api/v1/whitelist/requests/{requestId}/approve} · permission {@code players.write}
     *
     * @param requestId request id, e.g. {@code R1234}
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult approve(String requestId, RequestOptions options) {
        return inv.call(Calls.whitelist_approve(requestId), options);
    }

    /**
     * Denies a whitelist request. Note: txAdmin reports success even for an unknown id.
     *
     * <p>{@code POST /api/v1/whitelist/requests/{requestId}/deny} · permission {@code players.write}
     *
     * @param requestId request id, e.g. {@code R1234}
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult deny(String requestId) {
        return inv.call(Calls.whitelist_deny(requestId), null);
    }

    /**
     * Denies a whitelist request. Note: txAdmin reports success even for an unknown id.
     *
     * <p>{@code POST /api/v1/whitelist/requests/{requestId}/deny} · permission {@code players.write}
     *
     * @param requestId request id, e.g. {@code R1234}
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult deny(String requestId, RequestOptions options) {
        return inv.call(Calls.whitelist_deny(requestId), options);
    }

    /**
     * Denies every pending request that exists now.
     *
     * <p>{@code POST /api/v1/whitelist/requests/deny-all} · permission {@code players.write}
     *
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult denyAll() {
        return inv.call(Calls.whitelist_da0(), null);
    }

    /**
     * Denies every pending request that exists now.
     *
     * <p>{@code POST /api/v1/whitelist/requests/deny-all} · permission {@code players.write}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult denyAll(RequestOptions options) {
        return inv.call(Calls.whitelist_da0(), options);
    }

    /**
     * Denies every pending request up to a point in time — requests that arrived after you loaded the list stay.
     *
     * <p>{@code POST /api/v1/whitelist/requests/deny-all} · permission {@code players.write}
     *
     * @param before newest request time to deny, epoch seconds (milliseconds are recognised too); {@code null} = now
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult denyAll(Long before) {
        return inv.call(Calls.whitelist_da1(before), null);
    }

    /**
     * Denies every pending request up to a point in time — requests that arrived after you loaded the list stay.
     *
     * <p>{@code POST /api/v1/whitelist/requests/deny-all} · permission {@code players.write}
     *
     * @param before newest request time to deny, epoch seconds (milliseconds are recognised too); {@code null} = now
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult denyAll(Long before, RequestOptions options) {
        return inv.call(Calls.whitelist_da1(before), options);
    }

    /**
     * Pre-approved identifiers ({@code /whitelist/approvals}).
     *
     * @return the approvals group (same per-call defaults as this group)
     */
    public WhitelistApprovals approvals() {
        return approvals;
    }
}
