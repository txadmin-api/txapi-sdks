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
 * Whitelist requests and pre-approvals ({@code /whitelist/…}). Every method returns a {@link CompletableFuture} and never blocks the caller.
 *
 * <p>Obtain it from {@code client.async().whitelist()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see Whitelist
 */
@SuppressWarnings("unused")
public final class AsyncWhitelist {
    private final Invoker inv;
    private final AsyncWhitelistApprovals approvals;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public AsyncWhitelist(Invoker invoker) {
        this.inv = invoker;
        this.approvals = new AsyncWhitelistApprovals(invoker);
    }

    /**
     * Pending whitelist requests (first page), passed through from txAdmin.
     *
     * <p>{@code GET /api/v1/whitelist/requests} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link Whitelist#requests}; errors complete the future exceptionally.
     *
     * @return a future completing with the requests (raw JSON)
     */
    public CompletableFuture<JsonNode> requests() {
        return inv.callAsync(Calls.whitelist_req0(), null);
    }

    /**
     * Pending whitelist requests (first page), passed through from txAdmin.
     *
     * <p>{@code GET /api/v1/whitelist/requests} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link Whitelist#requests}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the requests (raw JSON)
     */
    public CompletableFuture<JsonNode> requests(RequestOptions options) {
        return inv.callAsync(Calls.whitelist_req0(), options);
    }

    /**
     * Pending whitelist requests, one page, passed through from txAdmin.
     *
     * <p>{@code GET /api/v1/whitelist/requests} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link Whitelist#requests}; errors complete the future exceptionally.
     *
     * @param page page number (txAdmin's paging); {@code null} = first
     * @return a future completing with the requests (raw JSON)
     */
    public CompletableFuture<JsonNode> requests(Integer page) {
        return inv.callAsync(Calls.whitelist_req1(page), null);
    }

    /**
     * Pending whitelist requests, one page, passed through from txAdmin.
     *
     * <p>{@code GET /api/v1/whitelist/requests} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link Whitelist#requests}; errors complete the future exceptionally.
     *
     * @param page page number (txAdmin's paging); {@code null} = first
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the requests (raw JSON)
     */
    public CompletableFuture<JsonNode> requests(Integer page, RequestOptions options) {
        return inv.callAsync(Calls.whitelist_req1(page), options);
    }

    /**
     * Approves a whitelist request.
     *
     * <p>{@code POST /api/v1/whitelist/requests/{requestId}/approve} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link Whitelist#approve}; errors complete the future exceptionally.
     *
     * @param requestId request id, e.g. {@code R1234}
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> approve(String requestId) {
        return inv.callAsync(Calls.whitelist_approve(requestId), null);
    }

    /**
     * Approves a whitelist request.
     *
     * <p>{@code POST /api/v1/whitelist/requests/{requestId}/approve} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link Whitelist#approve}; errors complete the future exceptionally.
     *
     * @param requestId request id, e.g. {@code R1234}
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> approve(String requestId, RequestOptions options) {
        return inv.callAsync(Calls.whitelist_approve(requestId), options);
    }

    /**
     * Denies a whitelist request. Note: txAdmin reports success even for an unknown id.
     *
     * <p>{@code POST /api/v1/whitelist/requests/{requestId}/deny} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link Whitelist#deny}; errors complete the future exceptionally.
     *
     * @param requestId request id, e.g. {@code R1234}
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> deny(String requestId) {
        return inv.callAsync(Calls.whitelist_deny(requestId), null);
    }

    /**
     * Denies a whitelist request. Note: txAdmin reports success even for an unknown id.
     *
     * <p>{@code POST /api/v1/whitelist/requests/{requestId}/deny} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link Whitelist#deny}; errors complete the future exceptionally.
     *
     * @param requestId request id, e.g. {@code R1234}
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> deny(String requestId, RequestOptions options) {
        return inv.callAsync(Calls.whitelist_deny(requestId), options);
    }

    /**
     * Denies every pending request that exists now.
     *
     * <p>{@code POST /api/v1/whitelist/requests/deny-all} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link Whitelist#denyAll}; errors complete the future exceptionally.
     *
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> denyAll() {
        return inv.callAsync(Calls.whitelist_da0(), null);
    }

    /**
     * Denies every pending request that exists now.
     *
     * <p>{@code POST /api/v1/whitelist/requests/deny-all} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link Whitelist#denyAll}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> denyAll(RequestOptions options) {
        return inv.callAsync(Calls.whitelist_da0(), options);
    }

    /**
     * Denies every pending request up to a point in time — requests that arrived after you loaded the list stay.
     *
     * <p>{@code POST /api/v1/whitelist/requests/deny-all} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link Whitelist#denyAll}; errors complete the future exceptionally.
     *
     * @param before newest request time to deny, epoch seconds (milliseconds are recognised too); {@code null} = now
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> denyAll(Long before) {
        return inv.callAsync(Calls.whitelist_da1(before), null);
    }

    /**
     * Denies every pending request up to a point in time — requests that arrived after you loaded the list stay.
     *
     * <p>{@code POST /api/v1/whitelist/requests/deny-all} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link Whitelist#denyAll}; errors complete the future exceptionally.
     *
     * @param before newest request time to deny, epoch seconds (milliseconds are recognised too); {@code null} = now
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> denyAll(Long before, RequestOptions options) {
        return inv.callAsync(Calls.whitelist_da1(before), options);
    }

    /**
     * Pre-approved identifiers ({@code /whitelist/approvals}).
     *
     * @return the approvals group (same per-call defaults as this group)
     */
    public AsyncWhitelistApprovals approvals() {
        return approvals;
    }
}
