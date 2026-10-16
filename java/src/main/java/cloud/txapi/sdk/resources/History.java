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
 * Bans and warnings ({@code /history/…}, {@code /txadmin/ban-templates}).
 *
 * <p>Obtain it from {@code client.history()}; do not construct it yourself.
 * Every method has an overload with a trailing {@link RequestOptions} for per-call settings
 * such as another {@code serverId}.
 *
 * @see AsyncHistory
 */
@SuppressWarnings("unused")
public final class History {
    private final Invoker inv;

    /**
     * Internal: created by the client.
     *
     * @param invoker internal call runner
     */
    public History(Invoker invoker) {
        this.inv = invoker;

    }

    /**
     * One page of bans, newest first. Use {@link #bansAll()} for all pages.
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * @return one page of bans
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage bans() {
        return inv.call(Calls.history_bans0(), null);
    }

    /**
     * One page of bans, newest first. Use {@link #bansAll()} for all pages.
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of bans
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage bans(RequestOptions options) {
        return inv.call(Calls.history_bans0(), options);
    }

    /**
     * One page of bans.
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @return one page of bans
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage bans(HistoryPageParams params) {
        return inv.call(Calls.history_bans1(params), null);
    }

    /**
     * One page of bans.
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of bans
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage bans(HistoryPageParams params, RequestOptions options) {
        return inv.call(Calls.history_bans1(params), options);
    }

    /**
     * Iterates over every page of bans (lazy).
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> bansAll(HistoryPageParams params) {
        return bansAll(params, (RequestOptions) null);
    }

    /**
     * Iterates over every page of bans (lazy).
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> bansAll(HistoryPageParams params, RequestOptions options) {
        HistoryPageParams base = params == null ? HistoryPageParams.create() : params;
        return Pager.sync(token -> {
            HistoryPageParams p = token == null ? base : base.page((PageCursor) token);
            return Tokens.history(inv.call(Calls.history_bans1(p), options));
        });
    }

    /**
     * Iterates over all bans, newest first (lazy).
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> bansAll() {
        return bansAll((RequestOptions) null);
    }

    /**
     * Iterates over all bans, newest first (lazy).
     *
     * <p>{@code GET /api/v1/history/bans} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> bansAll(RequestOptions options) {
        return bansAll((HistoryPageParams) null, options);
    }

    /**
     * One page of warnings, newest first. Use {@link #warnsAll()} for all pages.
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * @return one page of warnings
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage warns() {
        return inv.call(Calls.history_warns0(), null);
    }

    /**
     * One page of warnings, newest first. Use {@link #warnsAll()} for all pages.
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of warnings
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage warns(RequestOptions options) {
        return inv.call(Calls.history_warns0(), options);
    }

    /**
     * One page of warnings.
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @return one page of warnings
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage warns(HistoryPageParams params) {
        return inv.call(Calls.history_warns1(params), null);
    }

    /**
     * One page of warnings.
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of warnings
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage warns(HistoryPageParams params, RequestOptions options) {
        return inv.call(Calls.history_warns1(params), options);
    }

    /**
     * Iterates over every page of warnings (lazy).
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> warnsAll(HistoryPageParams params) {
        return warnsAll(params, (RequestOptions) null);
    }

    /**
     * Iterates over every page of warnings (lazy).
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * @param params sort order and page position; {@code null} = newest first, first page
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> warnsAll(HistoryPageParams params, RequestOptions options) {
        HistoryPageParams base = params == null ? HistoryPageParams.create() : params;
        return Pager.sync(token -> {
            HistoryPageParams p = token == null ? base : base.page((PageCursor) token);
            return Tokens.history(inv.call(Calls.history_warns1(p), options));
        });
    }

    /**
     * Iterates over all warnings, newest first (lazy).
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> warnsAll() {
        return warnsAll((RequestOptions) null);
    }

    /**
     * Iterates over all warnings, newest first (lazy).
     *
     * <p>{@code GET /api/v1/history/warns} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> warnsAll(RequestOptions options) {
        return warnsAll((HistoryPageParams) null, options);
    }

    /**
     * Searches bans and warnings with every filter txAdmin offers (one page). Use {@link #searchAll(HistorySearchParams)} for all pages.
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * @return one page of actions
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage search() {
        return inv.call(Calls.history_search0(), null);
    }

    /**
     * Searches bans and warnings with every filter txAdmin offers (one page). Use {@link #searchAll(HistorySearchParams)} for all pages.
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of actions
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage search(RequestOptions options) {
        return inv.call(Calls.history_search0(), options);
    }

    /**
     * Searches bans and warnings (type, admin, action id / reason / identifier search). One page.
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * @param params filters, search text, order and page; {@code null} = everything
     * @return one page of actions
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage search(HistorySearchParams params) {
        return inv.call(Calls.history_search1(params), null);
    }

    /**
     * Searches bans and warnings (type, admin, action id / reason / identifier search). One page.
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * @param params filters, search text, order and page; {@code null} = everything
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return one page of actions
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public HistoryPage search(HistorySearchParams params, RequestOptions options) {
        return inv.call(Calls.history_search1(params), options);
    }

    /**
     * Iterates over every page of {@link #search(HistorySearchParams)} (lazy).
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * @param params filters, search text, order and page; {@code null} = everything
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> searchAll(HistorySearchParams params) {
        return searchAll(params, (RequestOptions) null);
    }

    /**
     * Iterates over every page of {@link #search(HistorySearchParams)} (lazy).
     *
     * <p>{@code GET /api/v1/history/search} · permission {@code players.read}
     *
     * @param params filters, search text, order and page; {@code null} = everything
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return a lazy iterable over every row
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public PageIterable<HistoryEntry> searchAll(HistorySearchParams params, RequestOptions options) {
        HistorySearchParams base = params == null ? HistorySearchParams.create() : params;
        return Pager.sync(token -> {
            HistorySearchParams p = token == null ? base : base.page((PageCursor) token);
            return Tokens.history(inv.call(Calls.history_search1(p), options));
        });
    }

    /**
     * The ban templates configured in txAdmin (reason + duration), e.g. to offer them in your own tool.
     *
     * <p>{@code GET /api/v1/txadmin/ban-templates} · permission {@code players.read}
     *
     * @return the templates (empty list if none)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public List<BanTemplate> banTemplates() {
        return inv.call(Calls.history_banTemplates(), null);
    }

    /**
     * The ban templates configured in txAdmin (reason + duration), e.g. to offer them in your own tool.
     *
     * <p>{@code GET /api/v1/txadmin/ban-templates} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the templates (empty list if none)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public List<BanTemplate> banTemplates(RequestOptions options) {
        return inv.call(Calls.history_banTemplates(), options);
    }

    /**
     * Details of one ban or warning as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/history/action/{actionId}} · permission {@code players.read}
     *
     * @param actionId action id, e.g. {@code ABCD-1234}
     * @return the action (raw JSON)
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode action(String actionId) {
        return inv.call(Calls.history_action(actionId), null);
    }

    /**
     * Details of one ban or warning as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/history/action/{actionId}} · permission {@code players.read}
     *
     * @param actionId action id, e.g. {@code ABCD-1234}
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the action (raw JSON)
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode action(String actionId, RequestOptions options) {
        return inv.call(Calls.history_action(actionId), options);
    }

    /**
     * Ban and warning statistics as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/history/stats} · permission {@code players.read}
     *
     * @return the statistics (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode stats() {
        return inv.call(Calls.history_stats(), null);
    }

    /**
     * Ban and warning statistics as txAdmin returns them, passed through unchanged.
     *
     * <p>{@code GET /api/v1/history/stats} · permission {@code players.read}
     *
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return the statistics (raw JSON)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public JsonNode stats(RequestOptions options) {
        return inv.call(Calls.history_stats(), options);
    }

    /**
     * Revokes (lifts) a ban or warning without a reason.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult revoke(String actionId) {
        return inv.call(Calls.history_revoke1(actionId), null);
    }

    /**
     * Revokes (lifts) a ban or warning without a reason.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult revoke(String actionId, RequestOptions options) {
        return inv.call(Calls.history_revoke1(actionId), options);
    }

    /**
     * Revokes (lifts) a ban or warning with a reason.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param reason why it is revoked
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult revoke(String actionId, String reason) {
        return inv.call(Calls.history_revoke2(actionId, reason), null);
    }

    /**
     * Revokes (lifts) a ban or warning with a reason.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param reason why it is revoked
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success} and txAdmin's message
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult revoke(String actionId, String reason, RequestOptions options) {
        return inv.call(Calls.history_revoke2(actionId, reason), options);
    }

    /**
     * Revokes (lifts) a ban or warning. With {@link RevokeRequest#discordUnban()} the Discord ban on linked Discord servers is lifted too; the outcome is in {@link ActionResult#discord()}.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param request reason and Discord unban; {@code null} = neither
     * @return {@code success}, txAdmin's message and (with {@code discordUnban}) the Discord result
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult revoke(String actionId, RevokeRequest request) {
        return inv.call(Calls.history_revoke3(actionId, request), null);
    }

    /**
     * Revokes (lifts) a ban or warning. With {@link RevokeRequest#discordUnban()} the Discord ban on linked Discord servers is lifted too; the outcome is in {@link ActionResult#discord()}.
     *
     * <p>{@code POST /api/v1/history/revoke/{actionId}} · permission {@code players.write}
     *
     * @param actionId id of the ban or warning, e.g. {@code ABCD-1234}
     * @param request reason and Discord unban; {@code null} = neither
     * @param options per-call options (server, timeout, retries); {@code null} = defaults of the client/view
     * @return {@code success}, txAdmin's message and (with {@code discordUnban}) the Discord result
     * @throws cloud.txapi.sdk.errors.ValidationException if a required value is missing or blank, or too long (no request is sent)
     * @throws cloud.txapi.sdk.errors.TxApiException on API errors (see {@link cloud.txapi.sdk.errors}) and transport problems
     */
    public ActionResult revoke(String actionId, RevokeRequest request, RequestOptions options) {
        return inv.call(Calls.history_revoke3(actionId, request), options);
    }
}
