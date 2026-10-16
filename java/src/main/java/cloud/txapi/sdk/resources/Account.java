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
 * Account-level calls that do not address a txAdmin server ({@code GET /me}, {@code GET /servers}). Work with every valid key.
 *
 * <p>Obtain it from {@code client.account()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncAccount
 */
@SuppressWarnings("unused")
public final class Account {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public Account(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * Returns the plan, the effective features (plan ∩ key scopes), the monthly quota and the key itself. Useful to check what a key may do before calling anything else; also works when the monthly quota is used up.
     *
     * <p>{@code GET /api/v1/me} · any valid key (account-level, no server)
     *
     * @return account, plan, quota and key information
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AccountInfo me() {
        return inv.call(Calls.account_me(), null);
    }

    /**
     * Returns the plan, the effective features (plan ∩ key scopes), the monthly quota and the key itself. Useful to check what a key may do before calling anything else; also works when the monthly quota is used up.
     *
     * <p>{@code GET /api/v1/me} · any valid key (account-level, no server)
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return account, plan, quota and key information
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public AccountInfo me(RequestOptions options) {
        return inv.call(Calls.account_me(), options);
    }

    /**
     * Lists the txAdmin servers of the account (only the bound server for a server-bound key), with health and switches — never URLs or credentials.
     *
     * <p>{@code GET /api/v1/servers} · any valid key (account-level, no server)
     *
     * @return the servers
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerList servers() {
        return inv.call(Calls.account_servers(), null);
    }

    /**
     * Lists the txAdmin servers of the account (only the bound server for a server-bound key), with health and switches — never URLs or credentials.
     *
     * <p>{@code GET /api/v1/servers} · any valid key (account-level, no server)
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the servers
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ServerList servers(RequestOptions options) {
        return inv.call(Calls.account_servers(), options);
    }
}
