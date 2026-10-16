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
 * Account-level calls that do not address a txAdmin server ({@code GET /me}, {@code GET /servers}). Work with every valid key. Every method returns a {@link CompletableFuture} and never blocks the caller.
 *
 * <p>Obtain it from {@code client.async().account()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see Account
 */
@SuppressWarnings("unused")
public final class AsyncAccount {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public AsyncAccount(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * Returns the plan, the effective features (plan ∩ key scopes), the monthly quota and the key itself. Useful to check what a key may do before calling anything else; also works when the monthly quota is used up.
     *
     * <p>{@code GET /api/v1/me} · any valid key (account-level, no server)
     *
     * <p>Asynchronous variant of {@link Account#me}; errors complete the future exceptionally.
     *
     * @return a future completing with account, plan, quota and key information
     */
    public CompletableFuture<AccountInfo> me() {
        return inv.callAsync(Calls.account_me(), null);
    }

    /**
     * Returns the plan, the effective features (plan ∩ key scopes), the monthly quota and the key itself. Useful to check what a key may do before calling anything else; also works when the monthly quota is used up.
     *
     * <p>{@code GET /api/v1/me} · any valid key (account-level, no server)
     *
     * <p>Asynchronous variant of {@link Account#me}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with account, plan, quota and key information
     */
    public CompletableFuture<AccountInfo> me(RequestOptions options) {
        return inv.callAsync(Calls.account_me(), options);
    }

    /**
     * Lists the txAdmin servers of the account (only the bound server for a server-bound key), with health and switches — never URLs or credentials.
     *
     * <p>{@code GET /api/v1/servers} · any valid key (account-level, no server)
     *
     * <p>Asynchronous variant of {@link Account#servers}; errors complete the future exceptionally.
     *
     * @return a future completing with the servers
     */
    public CompletableFuture<ServerList> servers() {
        return inv.callAsync(Calls.account_servers(), null);
    }

    /**
     * Lists the txAdmin servers of the account (only the bound server for a server-bound key), with health and switches — never URLs or credentials.
     *
     * <p>{@code GET /api/v1/servers} · any valid key (account-level, no server)
     *
     * <p>Asynchronous variant of {@link Account#servers}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the servers
     */
    public CompletableFuture<ServerList> servers(RequestOptions options) {
        return inv.callAsync(Calls.account_servers(), options);
    }
}
