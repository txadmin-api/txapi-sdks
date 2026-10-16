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

/** Call builders shared by the sync and async resource groups. Generated — do not edit by hand. */
@SuppressWarnings("unused")
final class Calls {
    private Calls() {
    }

    static Call<AccountInfo> account_me() {
        Call<AccountInfo> c = Call.of(Endpoint.ACCOUNT_ME, AccountInfo.class);

        return c;
    }

    static Call<ServerList> account_servers() {
        Call<ServerList> c = Call.of(Endpoint.ACCOUNT_SERVERS, ServerList.class);

        return c;
    }

    static Call<PlayerPage> players_search0() {
        Call<PlayerPage> c = Call.of(Endpoint.PLAYERS_SEARCH, PlayerPage.class);

        return c;
    }

    static Call<PlayerPage> players_search1(PlayerSearchParams params) {
        Call<PlayerPage> c = Call.of(Endpoint.PLAYERS_SEARCH, PlayerPage.class);
        if (params != null) c.query(params.toQuery());
        return c;
    }

    static Call<PlayerPage> players_online0() {
        Call<PlayerPage> c = Call.of(Endpoint.PLAYERS_ONLINE, PlayerPage.class);

        return c;
    }

    static Call<PlayerPage> players_online1(PageCursor page) {
        Call<PlayerPage> c = Call.of(Endpoint.PLAYERS_ONLINE, PlayerPage.class);
        if (page != null) { c.query("offset", page.offset()); c.query("cursor", page.cursor()); }
        return c;
    }

    static Call<PlayerPage> players_lookup0(String identifier) {
        Call<PlayerPage> c = Call.of(Endpoint.PLAYERS_LOOKUP, PlayerPage.class);
        c.query("identifier", identifier);
        return c;
    }

    static Call<PlayerPage> players_lookup1(String identifier, PageCursor page) {
        Call<PlayerPage> c = Call.of(Endpoint.PLAYERS_LOOKUP, PlayerPage.class);
        c.query("identifier", identifier);
        if (page != null) { c.query("offset", page.offset()); c.query("cursor", page.cursor()); }
        return c;
    }

    static Call<JsonNode> players_info(String identifier) {
        Call<JsonNode> c = Call.of(Endpoint.PLAYERS_INFO, JsonNode.class);
        c.query("identifier", identifier);
        return c;
    }

    static Call<JsonNode> players_stats() {
        Call<JsonNode> c = Call.of(Endpoint.PLAYERS_STATS, JsonNode.class);

        return c;
    }

    static Call<ActionResult> players_warn(String identifier, String reason) {
        Call<ActionResult> c = Call.of(Endpoint.PLAYERS_WARN, ActionResult.class);
        c.body("identifier", identifier);
        c.body("reason", reason);
        return c;
    }

    static Call<ActionResult> players_ban1(BanRequest request) {
        Call<ActionResult> c = Call.of(Endpoint.PLAYERS_BAN, ActionResult.class);
        java.util.Objects.requireNonNull(request, "request");
        c.body("identifier", request.identifier());
        c.body("reason", request.reason());
        c.body("durationHours", request.durationHours());
        c.body("discordBan", request.discordBan());
        return c;
    }

    static Call<ActionResult> players_ban2(String identifier, String reason) {
        Call<ActionResult> c = Call.of(Endpoint.PLAYERS_BAN, ActionResult.class);
        c.body("identifier", identifier);
        c.body("reason", reason);
        return c;
    }

    static Call<ActionResult> players_kick1(String identifier) {
        Call<ActionResult> c = Call.of(Endpoint.PLAYERS_KICK, ActionResult.class);
        c.body("identifier", identifier);
        return c;
    }

    static Call<ActionResult> players_kick2(String identifier, String reason) {
        Call<ActionResult> c = Call.of(Endpoint.PLAYERS_KICK, ActionResult.class);
        c.body("identifier", identifier);
        c.body("reason", reason);
        return c;
    }

    static Call<ActionResult> players_dm(String identifier, String message) {
        Call<ActionResult> c = Call.of(Endpoint.PLAYERS_DM, ActionResult.class);
        c.body("identifier", identifier);
        c.body("message", message);
        return c;
    }

    static Call<ActionResult> players_setNote(String identifier, String note) {
        Call<ActionResult> c = Call.of(Endpoint.PLAYERS_NOTES, ActionResult.class);
        c.body("identifier", identifier);
        c.body("note", note);
        return c;
    }

    static Call<ActionResult> players_setWhitelisted(String identifier, boolean whitelisted) {
        Call<ActionResult> c = Call.of(Endpoint.PLAYERS_WHITELIST, ActionResult.class);
        c.body("identifier", identifier);
        c.body("whitelisted", whitelisted);
        return c;
    }

    static Call<HistoryPage> history_bans0() {
        Call<HistoryPage> c = Call.of(Endpoint.HISTORY_BANS, HistoryPage.class);

        return c;
    }

    static Call<HistoryPage> history_bans1(HistoryPageParams params) {
        Call<HistoryPage> c = Call.of(Endpoint.HISTORY_BANS, HistoryPage.class);
        if (params != null) c.query(params.toQuery());
        return c;
    }

    static Call<HistoryPage> history_warns0() {
        Call<HistoryPage> c = Call.of(Endpoint.HISTORY_WARNS, HistoryPage.class);

        return c;
    }

    static Call<HistoryPage> history_warns1(HistoryPageParams params) {
        Call<HistoryPage> c = Call.of(Endpoint.HISTORY_WARNS, HistoryPage.class);
        if (params != null) c.query(params.toQuery());
        return c;
    }

    static Call<HistoryPage> history_search0() {
        Call<HistoryPage> c = Call.of(Endpoint.HISTORY_SEARCH, HistoryPage.class);

        return c;
    }

    static Call<HistoryPage> history_search1(HistorySearchParams params) {
        Call<HistoryPage> c = Call.of(Endpoint.HISTORY_SEARCH, HistoryPage.class);
        if (params != null) c.query(params.toQuery());
        return c;
    }

    static Call<List<BanTemplate>> history_banTemplates() {
        Call<List<BanTemplate>> c = Call.of(Endpoint.HISTORY_BAN_TEMPLATES, new TypeReference<List<BanTemplate>>() { });

        return c;
    }

    static Call<JsonNode> history_action(String actionId) {
        Call<JsonNode> c = Call.of(Endpoint.HISTORY_ACTION, JsonNode.class);
        c.path("actionId", actionId);
        return c;
    }

    static Call<JsonNode> history_stats() {
        Call<JsonNode> c = Call.of(Endpoint.HISTORY_STATS, JsonNode.class);

        return c;
    }

    static Call<ActionResult> history_revoke1(String actionId) {
        Call<ActionResult> c = Call.of(Endpoint.HISTORY_REVOKE, ActionResult.class);
        c.path("actionId", actionId);
        return c;
    }

    static Call<ActionResult> history_revoke2(String actionId, String reason) {
        Call<ActionResult> c = Call.of(Endpoint.HISTORY_REVOKE, ActionResult.class);
        c.path("actionId", actionId);
        c.body("reason", reason);
        return c;
    }

    static Call<ActionResult> history_revoke3(String actionId, RevokeRequest request) {
        Call<ActionResult> c = Call.of(Endpoint.HISTORY_REVOKE, ActionResult.class);
        c.path("actionId", actionId);
        if (request != null) { c.body("reason", request.reason()); c.body("discordUnban", request.discordUnban()); }
        return c;
    }

    static Call<ServerStatus> server_status() {
        Call<ServerStatus> c = Call.of(Endpoint.SERVER_STATUS, ServerStatus.class);

        return c;
    }

    static Call<JsonNode> server_diagnostics() {
        Call<JsonNode> c = Call.of(Endpoint.SERVER_DIAGNOSTICS, JsonNode.class);

        return c;
    }

    static Call<JsonNode> server_drops() {
        Call<JsonNode> c = Call.of(Endpoint.SERVER_DROPS, JsonNode.class);

        return c;
    }

    static Call<JsonNode> server_perf0() {
        Call<JsonNode> c = Call.of(Endpoint.SERVER_PERFORMANCE, JsonNode.class);

        return c;
    }

    static Call<JsonNode> server_perf1(String thread) {
        Call<JsonNode> c = Call.of(Endpoint.SERVER_PERFORMANCE, JsonNode.class);
        c.query("thread", thread);
        return c;
    }

    static Call<ActionResult> server_restart() {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_RESTART, ActionResult.class);

        return c;
    }

    static Call<ActionResult> server_start() {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_START, ActionResult.class);

        return c;
    }

    static Call<ActionResult> server_stop() {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_STOP, ActionResult.class);

        return c;
    }

    static Call<ActionResult> server_scheduleRestart(String time) {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_SCHEDULE_RESTART, ActionResult.class);
        c.body("time", time);
        return c;
    }

    static Call<ActionResult> server_skipRestart(boolean skip) {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_SKIP_RESTART, ActionResult.class);
        c.body("skip", skip);
        return c;
    }

    static Call<ActionResult> server_kickall0() {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_KICKALL, ActionResult.class);

        return c;
    }

    static Call<ActionResult> server_kickall1(String reason) {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_KICKALL, ActionResult.class);
        c.body("reason", reason);
        return c;
    }

    static Call<ActionResult> server_announce(String message) {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_ANNOUNCEMENT, ActionResult.class);
        c.body("message", message);
        return c;
    }

    static Call<ActionResult> serverResources_start(String name) {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_RESOURCE_START, ActionResult.class);
        c.body("name", name);
        return c;
    }

    static Call<ActionResult> serverResources_stop(String name) {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_RESOURCE_STOP, ActionResult.class);
        c.body("name", name);
        return c;
    }

    static Call<ActionResult> serverResources_restart(String name) {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_RESOURCE_RESTART, ActionResult.class);
        c.body("name", name);
        return c;
    }

    static Call<ActionResult> serverResources_ensure(String name) {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_RESOURCE_ENSURE, ActionResult.class);
        c.body("name", name);
        return c;
    }

    static Call<ActionResult> serverResources_refresh() {
        Call<ActionResult> c = Call.of(Endpoint.SERVER_RESOURCE_REFRESH, ActionResult.class);

        return c;
    }

    static Call<ConsoleBuffer> console_buffer() {
        Call<ConsoleBuffer> c = Call.of(Endpoint.CONSOLE_BUFFER, ConsoleBuffer.class);

        return c;
    }

    static Call<ActionResult> console_command(String command) {
        Call<ActionResult> c = Call.of(Endpoint.CONSOLE_COMMAND, ActionResult.class);
        c.body("command", command);
        return c;
    }

    static Call<ServerLogBuffer> serverLog_buffer() {
        Call<ServerLogBuffer> c = Call.of(Endpoint.SERVERLOG_BUFFER, ServerLogBuffer.class);

        return c;
    }

    static Call<ServerLogHistory> serverLog_hist0() {
        Call<ServerLogHistory> c = Call.of(Endpoint.SERVERLOG_HISTORY, ServerLogHistory.class);

        return c;
    }

    static Call<ServerLogHistory> serverLog_hist1(LogDirection direction, Long ref) {
        Call<ServerLogHistory> c = Call.of(Endpoint.SERVERLOG_HISTORY, ServerLogHistory.class);
        if (direction != null) c.query("direction", direction.value());
        c.query("ref", ref);
        return c;
    }

    static Call<EventPage> events_list0() {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_LIST, EventPage.class);

        return c;
    }

    static Call<EventPage> events_list1(EventListQuery query) {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_LIST, EventPage.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<EventPage> events_deaths0() {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_DEATHS, EventPage.class);

        return c;
    }

    static Call<EventPage> events_deaths1(DeathQuery query) {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_DEATHS, EventPage.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<EventPage> events_kills0() {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_KILLS, EventPage.class);

        return c;
    }

    static Call<EventPage> events_kills1(KillQuery query) {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_KILLS, EventPage.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<EventPage> events_chat0() {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_CHAT, EventPage.class);

        return c;
    }

    static Call<EventPage> events_chat1(ChatQuery query) {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_CHAT, EventPage.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<EventPage> events_commands0() {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_COMMANDS, EventPage.class);

        return c;
    }

    static Call<EventPage> events_commands1(CommandQuery query) {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_COMMANDS, EventPage.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<EventPage> events_adminActions0() {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_ADMIN_ACTIONS, EventPage.class);

        return c;
    }

    static Call<EventPage> events_adminActions1(AdminActionQuery query) {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_ADMIN_ACTIONS, EventPage.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<EventPage> events_actions0() {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_ACTIONS, EventPage.class);

        return c;
    }

    static Call<EventPage> events_actions1(ActionQuery query) {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_ACTIONS, EventPage.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<EventPage> events_sessions0() {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_SESSIONS, EventPage.class);

        return c;
    }

    static Call<EventPage> events_sessions1(SessionQuery query) {
        Call<EventPage> c = Call.of(Endpoint.EVENTS_SESSIONS, EventPage.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<Leaderboard> events_lb0() {
        Call<Leaderboard> c = Call.of(Endpoint.EVENTS_LEADERBOARD, Leaderboard.class);

        return c;
    }

    static Call<Leaderboard> events_lb1(LeaderboardQuery query) {
        Call<Leaderboard> c = Call.of(Endpoint.EVENTS_LEADERBOARD, Leaderboard.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<PlayerEventStats> events_pl0(String player) {
        Call<PlayerEventStats> c = Call.of(Endpoint.EVENTS_PLAYER, PlayerEventStats.class);
        c.query("player", player);
        return c;
    }

    static Call<PlayerEventStats> events_pl1(String player, String since) {
        Call<PlayerEventStats> c = Call.of(Endpoint.EVENTS_PLAYER, PlayerEventStats.class);
        c.query("player", player);
        c.query("since", since);
        return c;
    }

    static Call<EventSummary> events_sum0() {
        Call<EventSummary> c = Call.of(Endpoint.EVENTS_SUMMARY, EventSummary.class);

        return c;
    }

    static Call<EventSummary> events_sum1(String since) {
        Call<EventSummary> c = Call.of(Endpoint.EVENTS_SUMMARY, EventSummary.class);
        c.query("since", since);
        return c;
    }

    static Call<Playtime> events_pt0() {
        Call<Playtime> c = Call.of(Endpoint.EVENTS_PLAYTIME, Playtime.class);

        return c;
    }

    static Call<Playtime> events_pt1(PlaytimeQuery query) {
        Call<Playtime> c = Call.of(Endpoint.EVENTS_PLAYTIME, Playtime.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<Activity> events_act0() {
        Call<Activity> c = Call.of(Endpoint.EVENTS_ACTIVITY, Activity.class);

        return c;
    }

    static Call<Activity> events_act1(ActivityQuery query) {
        Call<Activity> c = Call.of(Endpoint.EVENTS_ACTIVITY, Activity.class);
        if (query != null) c.query(query.toQuery());
        return c;
    }

    static Call<ActionLog> events_al0() {
        Call<ActionLog> c = Call.of(Endpoint.EVENTS_ACTION_LOG, ActionLog.class);

        return c;
    }

    static Call<ActionLog> events_al1(Integer lines) {
        Call<ActionLog> c = Call.of(Endpoint.EVENTS_ACTION_LOG, ActionLog.class);
        c.query("lines", lines);
        return c;
    }

    static Call<JsonNode> whitelist_req0() {
        Call<JsonNode> c = Call.of(Endpoint.WHITELIST_REQUESTS, JsonNode.class);

        return c;
    }

    static Call<JsonNode> whitelist_req1(Integer page) {
        Call<JsonNode> c = Call.of(Endpoint.WHITELIST_REQUESTS, JsonNode.class);
        c.query("page", page);
        return c;
    }

    static Call<ActionResult> whitelist_approve(String requestId) {
        Call<ActionResult> c = Call.of(Endpoint.WHITELIST_APPROVE, ActionResult.class);
        c.path("requestId", requestId);
        return c;
    }

    static Call<ActionResult> whitelist_deny(String requestId) {
        Call<ActionResult> c = Call.of(Endpoint.WHITELIST_DENY, ActionResult.class);
        c.path("requestId", requestId);
        return c;
    }

    static Call<ActionResult> whitelist_da0() {
        Call<ActionResult> c = Call.of(Endpoint.WHITELIST_DENY_ALL, ActionResult.class);

        return c;
    }

    static Call<ActionResult> whitelist_da1(Long before) {
        Call<ActionResult> c = Call.of(Endpoint.WHITELIST_DENY_ALL, ActionResult.class);
        c.body("before", before);
        return c;
    }

    static Call<List<WhitelistApproval>> whitelistApprovals_list() {
        Call<List<WhitelistApproval>> c = Call.of(Endpoint.WHITELIST_APPROVALS, new TypeReference<List<WhitelistApproval>>() { });

        return c;
    }

    static Call<ActionResult> whitelistApprovals_add(String identifier) {
        Call<ActionResult> c = Call.of(Endpoint.WHITELIST_APPROVALS_ADD, ActionResult.class);
        c.body("identifier", identifier);
        return c;
    }

    static Call<ActionResult> whitelistApprovals_remove(String identifier) {
        Call<ActionResult> c = Call.of(Endpoint.WHITELIST_APPROVALS_REMOVE, ActionResult.class);
        c.path("identifier", identifier);
        return c;
    }

    static Call<AdminList> admins_list() {
        Call<AdminList> c = Call.of(Endpoint.ADMINS_LIST, AdminList.class);

        return c;
    }

    static Call<AdminDetails> admins_get(String name) {
        Call<AdminDetails> c = Call.of(Endpoint.ADMINS_GET, AdminDetails.class);
        c.path("name", name);
        return c;
    }

    static Call<AdminPermissions> admins_permissions() {
        Call<AdminPermissions> c = Call.of(Endpoint.ADMINS_PERMISSIONS, AdminPermissions.class);

        return c;
    }

    static Call<AdminCreated> admins_add1(AdminCreateRequest request) {
        Call<AdminCreated> c = Call.of(Endpoint.ADMINS_ADD, AdminCreated.class);
        java.util.Objects.requireNonNull(request, "request");
        c.body("name", request.name());
        c.body("citizenfxId", request.citizenfxId());
        c.body("discordId", request.discordId());
        c.body("permissions", request.permissions());
        return c;
    }

    static Call<AdminCreated> admins_add2(String name, List<String> permissions) {
        Call<AdminCreated> c = Call.of(Endpoint.ADMINS_ADD, AdminCreated.class);
        c.body("name", name);
        c.body("permissions", permissions == null ? null : List.copyOf(permissions));
        return c;
    }

    static Call<ActionResult> admins_edit(String name, AdminUpdateRequest request) {
        Call<ActionResult> c = Call.of(Endpoint.ADMINS_EDIT, ActionResult.class);
        c.path("name", name);
        if (request != null) { c.body("citizenfxId", request.citizenfxId()); c.body("discordId", request.discordId()); c.body("permissions", request.permissions()); }
        return c;
    }

    static Call<ActionResult> admins_delete(String name) {
        Call<ActionResult> c = Call.of(Endpoint.ADMINS_DELETE, ActionResult.class);
        c.path("name", name);
        return c;
    }
}
