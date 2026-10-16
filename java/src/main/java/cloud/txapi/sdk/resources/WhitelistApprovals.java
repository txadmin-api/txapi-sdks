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
 * Pre-approved identifiers: players with these identifiers pass the whitelist without a request.
 *
 * <p>Obtain it from {@code client.whitelistApprovals()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncWhitelistApprovals
 */
@SuppressWarnings("unused")
public final class WhitelistApprovals {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public WhitelistApprovals(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * Lists the pre-approved identifiers.
     *
     * <p>{@code GET /api/v1/whitelist/approvals} · permission {@code players.read}
     *
     * @return the approvals (empty list if none)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public List<WhitelistApproval> list() {
        return inv.call(Calls.whitelistApprovals_list(), null);
    }

    /**
     * Lists the pre-approved identifiers.
     *
     * <p>{@code GET /api/v1/whitelist/approvals} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the approvals (empty list if none)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public List<WhitelistApproval> list(RequestOptions options) {
        return inv.call(Calls.whitelistApprovals_list(), options);
    }

    /**
     * Pre-approves an identifier.
     *
     * <p>{@code POST /api/v1/whitelist/approvals} · permission {@code players.write}
     *
     * @param identifier typed identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} … (never {@code ip:})
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult add(String identifier) {
        return inv.call(Calls.whitelistApprovals_add(identifier), null);
    }

    /**
     * Pre-approves an identifier.
     *
     * <p>{@code POST /api/v1/whitelist/approvals} · permission {@code players.write}
     *
     * @param identifier typed identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} … (never {@code ip:})
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult add(String identifier, RequestOptions options) {
        return inv.call(Calls.whitelistApprovals_add(identifier), options);
    }

    /**
     * Removes a pre-approval.
     *
     * <p>{@code DELETE /api/v1/whitelist/approvals/{identifier}} · permission {@code players.write}
     *
     * @param identifier typed identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} … (never {@code ip:})
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult remove(String identifier) {
        return inv.call(Calls.whitelistApprovals_remove(identifier), null);
    }

    /**
     * Removes a pre-approval.
     *
     * <p>{@code DELETE /api/v1/whitelist/approvals/{identifier}} · permission {@code players.write}
     *
     * @param identifier typed identifier: {@code license:…}, {@code discord:…}, {@code fivem:…}, {@code steam:…} … (never {@code ip:})
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult remove(String identifier, RequestOptions options) {
        return inv.call(Calls.whitelistApprovals_remove(identifier), options);
    }
}
