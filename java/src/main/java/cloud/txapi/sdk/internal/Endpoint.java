package cloud.txapi.sdk.internal;

import java.util.List;

/**
 * The txApi REST surface, API version 1.8.0 — generated from {@code sdks/spec/endpoints.json}.
 * Internal: used for request building and client-side validation.
 */
public enum Endpoint {
    ACCOUNT_ME("account.me", "GET", "/api/v1/me", null, true, false, List.of(), List.of(), List.of()),
    ACCOUNT_SERVERS("account.servers", "GET", "/api/v1/servers", null, true, false, List.of(), List.of(), List.of()),
    PLAYERS_SEARCH("players.search", "GET", "/api/v1/players/search", "players.read", false, false, List.of(), List.of(), List.of()),
    PLAYERS_ONLINE("players.online", "GET", "/api/v1/players/online", "players.read", false, false, List.of(), List.of(), List.of()),
    PLAYERS_LOOKUP("players.lookup", "GET", "/api/v1/players/lookup", "players.read", false, false, List.of(), List.of("identifier"), List.of()),
    PLAYERS_INFO("players.info", "GET", "/api/v1/players/info", "players.read", false, false, List.of(), List.of("identifier"), List.of()),
    PLAYERS_STATS("players.stats", "GET", "/api/v1/players/stats", "players.read", false, false, List.of(), List.of(), List.of()),
    PLAYERS_WARN("players.warn", "POST", "/api/v1/players/warn", "players.write", false, true, List.of(), List.of(), List.of(f("identifier", true, -1), f("reason", true, 512))),
    PLAYERS_BAN("players.ban", "POST", "/api/v1/players/ban", "players.write", false, true, List.of(), List.of(), List.of(f("identifier", true, -1), f("reason", true, 512), f("durationHours", false, -1), f("discordBan", false, -1))),
    PLAYERS_KICK("players.kick", "POST", "/api/v1/players/kick", "players.write", false, true, List.of(), List.of(), List.of(f("identifier", true, -1), f("reason", false, 512))),
    PLAYERS_DM("players.dm", "POST", "/api/v1/players/dm", "players.write", false, true, List.of(), List.of(), List.of(f("identifier", true, -1), f("message", true, 512))),
    PLAYERS_NOTES("players.notes", "POST", "/api/v1/players/notes", "players.write", false, true, List.of(), List.of(), List.of(f("identifier", true, -1), f("note", false, 2000))),
    PLAYERS_WHITELIST("players.whitelist", "POST", "/api/v1/players/whitelist", "players.write", false, true, List.of(), List.of(), List.of(f("identifier", true, -1), f("whitelisted", true, -1))),
    HISTORY_BANS("history.bans", "GET", "/api/v1/history/bans", "players.read", false, false, List.of(), List.of(), List.of()),
    HISTORY_WARNS("history.warns", "GET", "/api/v1/history/warns", "players.read", false, false, List.of(), List.of(), List.of()),
    HISTORY_SEARCH("history.search", "GET", "/api/v1/history/search", "players.read", false, false, List.of(), List.of(), List.of()),
    HISTORY_BAN_TEMPLATES("history.ban-templates", "GET", "/api/v1/txadmin/ban-templates", "players.read", false, false, List.of(), List.of(), List.of()),
    HISTORY_ACTION("history.action", "GET", "/api/v1/history/action/{actionId}", "players.read", false, false, List.of("actionId"), List.of(), List.of()),
    HISTORY_STATS("history.stats", "GET", "/api/v1/history/stats", "players.read", false, false, List.of(), List.of(), List.of()),
    HISTORY_REVOKE("history.revoke", "POST", "/api/v1/history/revoke/{actionId}", "players.write", false, true, List.of("actionId"), List.of(), List.of(f("reason", false, 512), f("discordUnban", false, -1))),
    SERVER_STATUS("server.status", "GET", "/api/v1/server/status", "server.control", false, false, List.of(), List.of(), List.of()),
    SERVER_DIAGNOSTICS("server.diagnostics", "GET", "/api/v1/server/diagnostics", "server.control", false, false, List.of(), List.of(), List.of()),
    SERVER_DROPS("server.drops", "GET", "/api/v1/server/drops", "server.control", false, false, List.of(), List.of(), List.of()),
    SERVER_PERFORMANCE("server.performance", "GET", "/api/v1/server/performance", "server.control", false, false, List.of(), List.of(), List.of()),
    SERVER_RESTART("server.restart", "POST", "/api/v1/server/restart", "server.control", false, false, List.of(), List.of(), List.of()),
    SERVER_START("server.start", "POST", "/api/v1/server/start", "server.control", false, false, List.of(), List.of(), List.of()),
    SERVER_STOP("server.stop", "POST", "/api/v1/server/stop", "server.control", false, false, List.of(), List.of(), List.of()),
    SERVER_SCHEDULE_RESTART("server.schedule-restart", "POST", "/api/v1/server/schedule-restart", "server.control", false, true, List.of(), List.of(), List.of(f("time", true, -1))),
    SERVER_SKIP_RESTART("server.skip-restart", "POST", "/api/v1/server/skip-restart", "server.control", false, true, List.of(), List.of(), List.of(f("skip", true, -1))),
    SERVER_KICKALL("server.kickall", "POST", "/api/v1/server/kickall", "server.control", false, true, List.of(), List.of(), List.of(f("reason", false, 512))),
    SERVER_ANNOUNCEMENT("server.announcement", "POST", "/api/v1/server/announcement", "server.control", false, true, List.of(), List.of(), List.of(f("message", true, 512))),
    SERVER_RESOURCE_START("server.resource.start", "POST", "/api/v1/server/resource/start", "server.control", false, true, List.of(), List.of(), List.of(f("name", true, 128))),
    SERVER_RESOURCE_STOP("server.resource.stop", "POST", "/api/v1/server/resource/stop", "server.control", false, true, List.of(), List.of(), List.of(f("name", true, 128))),
    SERVER_RESOURCE_RESTART("server.resource.restart", "POST", "/api/v1/server/resource/restart", "server.control", false, true, List.of(), List.of(), List.of(f("name", true, 128))),
    SERVER_RESOURCE_ENSURE("server.resource.ensure", "POST", "/api/v1/server/resource/ensure", "server.control", false, true, List.of(), List.of(), List.of(f("name", true, 128))),
    SERVER_RESOURCE_REFRESH("server.resource.refresh", "POST", "/api/v1/server/resource/refresh", "server.control", false, false, List.of(), List.of(), List.of()),
    CONSOLE_BUFFER("console.buffer", "GET", "/api/v1/console", "console.write", false, false, List.of(), List.of(), List.of()),
    CONSOLE_COMMAND("console.command", "POST", "/api/v1/console/command", "console.write", false, true, List.of(), List.of(), List.of(f("command", true, 1024))),
    SERVERLOG_BUFFER("serverlog.buffer", "GET", "/api/v1/server/log", "logs.view", false, false, List.of(), List.of(), List.of()),
    SERVERLOG_HISTORY("serverlog.history", "GET", "/api/v1/server/log/history", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_LIST("events.list", "GET", "/api/v1/events", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_DEATHS("events.deaths", "GET", "/api/v1/events/deaths", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_KILLS("events.kills", "GET", "/api/v1/events/kills", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_LEADERBOARD("events.leaderboard", "GET", "/api/v1/events/leaderboard", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_PLAYER("events.player", "GET", "/api/v1/events/player", "logs.view", false, false, List.of(), List.of("player"), List.of()),
    EVENTS_SUMMARY("events.summary", "GET", "/api/v1/events/summary", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_CHAT("events.chat", "GET", "/api/v1/events/chat", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_COMMANDS("events.commands", "GET", "/api/v1/events/commands", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_ADMIN_ACTIONS("events.admin-actions", "GET", "/api/v1/events/admin-actions", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_ACTIONS("events.actions", "GET", "/api/v1/events/actions", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_SESSIONS("events.sessions", "GET", "/api/v1/events/sessions", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_PLAYTIME("events.playtime", "GET", "/api/v1/events/playtime", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_ACTIVITY("events.activity", "GET", "/api/v1/events/activity", "logs.view", false, false, List.of(), List.of(), List.of()),
    EVENTS_ACTION_LOG("events.action-log", "GET", "/api/v1/txadmin/action-log", "logs.view", false, false, List.of(), List.of(), List.of()),
    WHITELIST_REQUESTS("whitelist.requests", "GET", "/api/v1/whitelist/requests", "players.read", false, false, List.of(), List.of(), List.of()),
    WHITELIST_APPROVE("whitelist.approve", "POST", "/api/v1/whitelist/requests/{requestId}/approve", "players.write", false, false, List.of("requestId"), List.of(), List.of()),
    WHITELIST_DENY("whitelist.deny", "POST", "/api/v1/whitelist/requests/{requestId}/deny", "players.write", false, false, List.of("requestId"), List.of(), List.of()),
    WHITELIST_DENY_ALL("whitelist.deny-all", "POST", "/api/v1/whitelist/requests/deny-all", "players.write", false, true, List.of(), List.of(), List.of(f("before", false, -1))),
    WHITELIST_APPROVALS("whitelist.approvals", "GET", "/api/v1/whitelist/approvals", "players.read", false, false, List.of(), List.of(), List.of()),
    WHITELIST_APPROVALS_ADD("whitelist.approvals.add", "POST", "/api/v1/whitelist/approvals", "players.write", false, true, List.of(), List.of(), List.of(f("identifier", true, -1))),
    WHITELIST_APPROVALS_REMOVE("whitelist.approvals.remove", "DELETE", "/api/v1/whitelist/approvals/{identifier}", "players.write", false, false, List.of("identifier"), List.of(), List.of()),
    ADMINS_LIST("admins.list", "GET", "/api/v1/txadmin/admins", "admin.manage", false, false, List.of(), List.of(), List.of()),
    ADMINS_GET("admins.get", "GET", "/api/v1/txadmin/admins/{name}", "admin.manage", false, false, List.of("name"), List.of(), List.of()),
    ADMINS_PERMISSIONS("admins.permissions", "GET", "/api/v1/txadmin/admins/available-permissions", "admin.manage", false, false, List.of(), List.of(), List.of()),
    ADMINS_ADD("admins.add", "POST", "/api/v1/txadmin/admins", "admin.manage", false, true, List.of(), List.of(), List.of(f("name", true, -1), f("citizenfxId", false, -1), f("discordId", false, -1), f("permissions", true, -1))),
    ADMINS_EDIT("admins.edit", "PUT", "/api/v1/txadmin/admins/{name}", "admin.manage", false, true, List.of("name"), List.of(), List.of(f("citizenfxId", false, -1), f("discordId", false, -1), f("permissions", false, -1))),
    ADMINS_DELETE("admins.delete", "DELETE", "/api/v1/txadmin/admins/{name}", "admin.manage", false, false, List.of("name"), List.of(), List.of());

    /** A JSON body field with the client-side checks from the spec. */
    public record BodyField(String name, boolean required, int maxLength) {
    }

    private static BodyField f(String name, boolean required, int maxLength) {
        return new BodyField(name, required, maxLength);
    }

    private final String id;
    private final String method;
    private final String path;
    private final String permission;
    private final boolean accountLevel;
    private final boolean hasBody;
    private final List<String> pathParams;
    private final List<String> requiredQuery;
    private final List<BodyField> bodyFields;

    Endpoint(String id, String method, String path, String permission, boolean accountLevel, boolean hasBody,
             List<String> pathParams, List<String> requiredQuery, List<BodyField> bodyFields) {
        this.id = id;
        this.method = method;
        this.path = path;
        this.permission = permission;
        this.accountLevel = accountLevel;
        this.hasBody = hasBody;
        this.pathParams = pathParams;
        this.requiredQuery = requiredQuery;
        this.bodyFields = bodyFields;
    }

    public String id() { return id; }
    public String method() { return method; }
    /** Path template including {@code /api/v1}, e.g. {@code /api/v1/history/revoke/{actionId}}. */
    public String path() { return path; }
    /** Required plan feature, {@code null} = any valid key. */
    public String permission() { return permission; }
    /** Account-level endpoints take no {@code serverId}. */
    public boolean accountLevel() { return accountLevel; }
    /** Whether the endpoint takes a JSON body (sent as {@code {}} when every field is absent). */
    public boolean hasBody() { return hasBody; }
    public List<String> pathParams() { return pathParams; }
    public List<String> requiredQuery() { return requiredQuery; }
    public List<BodyField> bodyFields() { return bodyFields; }

    public static final String API_VERSION = "1.8.0";
}
