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
 * Bans and warnings ({@code /history/…}, {@code /txadmin/ban-templates}). Every method returns a {@link CompletableFuture} and never blocks the caller.
 *
 * <p>Obtain it from {@code client.async().history()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see History
 */
@SuppressWarnings("unused")
public final class AsyncHistory {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public AsyncHistory(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * One page of bans, newest first. Use {@link #bansAll()} for all pages.
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#bans}; errors complete the future exceptionally.
     *
     * @return a future completing with one page of bans
     */
    public CompletableFuture<HistoryPage> bans() {
        return inv.callAsync(Calls.history_bans0(), null);
    }

    /**
     * One page of bans, newest first. Use {@link #bansAll()} for all pages.
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#bans}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with one page of bans
     */
    public CompletableFuture<HistoryPage> bans(RequestOptions options) {
        return inv.callAsync(Calls.history_bans0(), options);
    }

    /**
     * One page of bans.
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#bans}; errors complete the future exceptionally.
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @return a future completing with one page of bans
     */
    public CompletableFuture<HistoryPage> bans(HistoryPageParams params) {
        return inv.callAsync(Calls.history_bans1(params), null);
    }

    /**
     * One page of bans.
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#bans}; errors complete the future exceptionally.
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with one page of bans
     */
    public CompletableFuture<HistoryPage> bans(HistoryPageParams params, RequestOptions options) {
        return inv.callAsync(Calls.history_bans1(params), options);
    }

    /**
     * Iterates over every page of bans (lazy).
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#bansAll}; errors complete the future exceptionally.
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> bansAll(HistoryPageParams params) {
        return bansAll(params, (RequestOptions) null);
    }

    /**
     * Iterates over every page of bans (lazy).
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#bansAll}; errors complete the future exceptionally.
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> bansAll(HistoryPageParams params, RequestOptions options) {
        HistoryPageParams base = params == null ? HistoryPageParams.create() : params;
        return Pager.async(token -> {
            HistoryPageParams p = token == null ? base : base.page((PageCursor) token);
            return inv.callAsync(Calls.history_bans1(p), options).thenApply(Tokens::history);
        });
    }

    /**
     * Iterates over all bans, newest first (lazy).
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#bansAll}; errors complete the future exceptionally.
     *
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> bansAll() {
        return bansAll((RequestOptions) null);
    }

    /**
     * Iterates over all bans, newest first (lazy).
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#bansAll}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> bansAll(RequestOptions options) {
        return bansAll((HistoryPageParams) null, options);
    }

    /**
     * One page of warnings, newest first. Use {@link #warnsAll()} for all pages.
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#warns}; errors complete the future exceptionally.
     *
     * @return a future completing with one page of warnings
     */
    public CompletableFuture<HistoryPage> warns() {
        return inv.callAsync(Calls.history_warns0(), null);
    }

    /**
     * One page of warnings, newest first. Use {@link #warnsAll()} for all pages.
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#warns}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with one page of warnings
     */
    public CompletableFuture<HistoryPage> warns(RequestOptions options) {
        return inv.callAsync(Calls.history_warns0(), options);
    }

    /**
     * One page of warnings.
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#warns}; errors complete the future exceptionally.
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @return a future completing with one page of warnings
     */
    public CompletableFuture<HistoryPage> warns(HistoryPageParams params) {
        return inv.callAsync(Calls.history_warns1(params), null);
    }

    /**
     * One page of warnings.
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#warns}; errors complete the future exceptionally.
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with one page of warnings
     */
    public CompletableFuture<HistoryPage> warns(HistoryPageParams params, RequestOptions options) {
        return inv.callAsync(Calls.history_warns1(params), options);
    }

    /**
     * Iterates over every page of warnings (lazy).
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#warnsAll}; errors complete the future exceptionally.
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> warnsAll(HistoryPageParams params) {
        return warnsAll(params, (RequestOptions) null);
    }

    /**
     * Iterates over every page of warnings (lazy).
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#warnsAll}; errors complete the future exceptionally.
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> warnsAll(HistoryPageParams params, RequestOptions options) {
        HistoryPageParams base = params == null ? HistoryPageParams.create() : params;
        return Pager.async(token -> {
            HistoryPageParams p = token == null ? base : base.page((PageCursor) token);
            return inv.callAsync(Calls.history_warns1(p), options).thenApply(Tokens::history);
        });
    }

    /**
     * Iterates over all warnings, newest first (lazy).
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#warnsAll}; errors complete the future exceptionally.
     *
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> warnsAll() {
        return warnsAll((RequestOptions) null);
    }

    /**
     * Iterates over all warnings, newest first (lazy).
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#warnsAll}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> warnsAll(RequestOptions options) {
        return warnsAll((HistoryPageParams) null, options);
    }

    /**
     * Searches bans and warnings with every filter txAdmin offers (one page). Use {@link #searchAll(HistorySearchParams)} for all pages.
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#search}; errors complete the future exceptionally.
     *
     * @return a future completing with one page of actions
     */
    public CompletableFuture<HistoryPage> search() {
        return inv.callAsync(Calls.history_search0(), null);
    }

    /**
     * Searches bans and warnings with every filter txAdmin offers (one page). Use {@link #searchAll(HistorySearchParams)} for all pages.
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#search}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with one page of actions
     */
    public CompletableFuture<HistoryPage> search(RequestOptions options) {
        return inv.callAsync(Calls.history_search0(), options);
    }

    /**
     * Searches bans and warnings (type, admin, action id / reason / identifier search). One page.
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#search}; errors complete the future exceptionally.
     *
     * @param params filters, search text, order and page; {@code null} = everything
     * @return a future completing with one page of actions
     */
    public CompletableFuture<HistoryPage> search(HistorySearchParams params) {
        return inv.callAsync(Calls.history_search1(params), null);
    }

    /**
     * Searches bans and warnings (type, admin, action id / reason / identifier search). One page.
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#search}; errors complete the future exceptionally.
     *
     * @param params filters, search text, order and page; {@code null} = everything
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with one page of actions
     */
    public CompletableFuture<HistoryPage> search(HistorySearchParams params, RequestOptions options) {
        return inv.callAsync(Calls.history_search1(params), options);
    }

    /**
     * Iterates over every page of {@link #search(HistorySearchParams)} (lazy).
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#searchAll}; errors complete the future exceptionally.
     *
     * @param params filters, search text, order and page; {@code null} = everything
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> searchAll(HistorySearchParams params) {
        return searchAll(params, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #search(HistorySearchParams)} (lazy).
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#searchAll}; errors complete the future exceptionally.
     *
     * @param params filters, search text, order and page; {@code null} = everything
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return an asynchronous lazy iterable over every row
     */
    public AsyncPageIterable<HistoryEntry> searchAll(HistorySearchParams params, RequestOptions options) {
        HistorySearchParams base = params == null ? HistorySearchParams.create() : params;
        return Pager.async(token -> {
            HistorySearchParams p = token == null ? base : base.page((PageCursor) token);
            return inv.callAsync(Calls.history_search1(p), options).thenApply(Tokens::history);
        });
    }

    /**
     * The ban templates configured in txAdmin (reason + duration), e.g. to offer them in your own tool.
     *
     * <p>{@code GET /api/v1/txadmin/ban-templates} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#banTemplates}; errors complete the future exceptionally.
     *
     * @return a future completing with the templates (empty list if none)
     */
    public CompletableFuture<List<BanTemplate>> banTemplates() {
        return inv.callAsync(Calls.history_banTemplates(), null);
    }

    /**
     * The ban templates configured in txAdmin (reason + duration), e.g. to offer them in your own tool.
     *
     * <p>{@code GET /api/v1/txadmin/ban-templates} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#banTemplates}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the templates (empty list if none)
     */
    public CompletableFuture<List<BanTemplate>> banTemplates(RequestOptions options) {
        return inv.callAsync(Calls.history_banTemplates(), options);
    }

    /**
     * Details of one ban or warning as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/history/action/{actionId}} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#action}; errors complete the future exceptionally.
     *
     * @param actionId action id, e.g. {@code ABCD-1234}
     * @return a future completing with the action (raw JSON)
     */
    public CompletableFuture<JsonNode> action(String actionId) {
        return inv.callAsync(Calls.history_action(actionId), null);
    }

    /**
     * Details of one ban or warning as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/history/action/{actionId}} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#action}; errors complete the future exceptionally.
     *
     * @param actionId action id, e.g. {@code ABCD-1234}
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the action (raw JSON)
     */
    public CompletableFuture<JsonNode> action(String actionId, RequestOptions options) {
        return inv.callAsync(Calls.history_action(actionId), options);
    }

    /**
     * Ban and warning statistics as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/history/stats} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#stats}; errors complete the future exceptionally.
     *
     * @return a future completing with the statistics (raw JSON)
     */
    public CompletableFuture<JsonNode> stats() {
        return inv.callAsync(Calls.history_stats(), null);
    }

    /**
     * Ban and warning statistics as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/history/stats} · permission {@code players.read}
     *
     * <p>Asynchronous variant of {@link History#stats}; errors complete the future exceptionally.
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with the statistics (raw JSON)
     */
    public CompletableFuture<JsonNode> stats(RequestOptions options) {
        return inv.callAsync(Calls.history_stats(), options);
    }

    /**
     * Revokes (lifts) a ban or warning without a reason.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link History#revoke}; errors complete the future exceptionally.
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> revoke(String actionId) {
        return inv.callAsync(Calls.history_revoke1(actionId), null);
    }

    /**
     * Revokes (lifts) a ban or warning without a reason.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link History#revoke}; errors complete the future exceptionally.
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> revoke(String actionId, RequestOptions options) {
        return inv.callAsync(Calls.history_revoke1(actionId), options);
    }

    /**
     * Revokes (lifts) a ban or warning with a reason.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link History#revoke}; errors complete the future exceptionally.
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param reason why it is revoked
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> revoke(String actionId, String reason) {
        return inv.callAsync(Calls.history_revoke2(actionId, reason), null);
    }

    /**
     * Revokes (lifts) a ban or warning with a reason.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link History#revoke}; errors complete the future exceptionally.
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param reason why it is revoked
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success} and txAdmin's message
     */
    public CompletableFuture<ActionResult> revoke(String actionId, String reason, RequestOptions options) {
        return inv.callAsync(Calls.history_revoke2(actionId, reason), options);
    }

    /**
     * Revokes (lifts) a ban or warning. With {@link RevokeRequest#discordUnban()} the Discord ban on linked Discord servers is lifted too; the outcome is in {@link ActionResult#discord()}.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link History#revoke}; errors complete the future exceptionally.
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param request reason and Discord unban; {@code null} = neither
     * @return a future completing with {@code success}, txAdmin's message and (with {@code discordUnban}) the Discord result
     */
    public CompletableFuture<ActionResult> revoke(String actionId, RevokeRequest request) {
        return inv.callAsync(Calls.history_revoke3(actionId, request), null);
    }

    /**
     * Revokes (lifts) a ban or warning. With {@link RevokeRequest#discordUnban()} the Discord ban on linked Discord servers is lifted too; the outcome is in {@link ActionResult#discord()}.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * <p>Asynchronous variant of {@link History#revoke}; errors complete the future exceptionally.
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param request reason and Discord unban; {@code null} = neither
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a future completing with {@code success}, txAdmin's message and (with {@code discordUnban}) the Discord result
     */
    public CompletableFuture<ActionResult> revoke(String actionId, RevokeRequest request, RequestOptions options) {
        return inv.callAsync(Calls.history_revoke3(actionId, request), options);
    }
}
