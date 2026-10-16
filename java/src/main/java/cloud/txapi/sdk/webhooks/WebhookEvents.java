package cloud.txapi.sdk.webhooks;

import java.util.List;

/**
 * Names of all webhook events the API emits (API version 1.8.0). Compare with
 * {@link WebhookEvent#event()} or {@link WebhookEvent#is(String)}.
 *
 * <p>New events can be added by the API at any time — treat unknown names as "ignore",
 * never as an error.
 */
public final class WebhookEvents {
    /** {@code action.revoked} */
    public static final String ACTION_REVOKED = "action.revoked";
    /** {@code console.command} */
    public static final String CONSOLE_COMMAND = "console.command";
    /** {@code log.admin_action} */
    public static final String LOG_ADMIN_ACTION = "log.admin_action";
    /** {@code log.ban} */
    public static final String LOG_BAN = "log.ban";
    /** {@code log.kill} */
    public static final String LOG_KILL = "log.kill";
    /** {@code log.warn} */
    public static final String LOG_WARN = "log.warn";
    /** {@code ping} */
    public static final String PING = "ping";
    /** {@code player.ban_synced} */
    public static final String PLAYER_BAN_SYNCED = "player.ban_synced";
    /** {@code player.banned} */
    public static final String PLAYER_BANNED = "player.banned";
    /** {@code player.kicked} */
    public static final String PLAYER_KICKED = "player.kicked";
    /** {@code player.messaged} */
    public static final String PLAYER_MESSAGED = "player.messaged";
    /** {@code player.note} */
    public static final String PLAYER_NOTE = "player.note";
    /** {@code player.warned} */
    public static final String PLAYER_WARNED = "player.warned";
    /** {@code player.whitelist} */
    public static final String PLAYER_WHITELIST = "player.whitelist";
    /** {@code quota.warning} */
    public static final String QUOTA_WARNING = "quota.warning";
    /** {@code schedule.ran} */
    public static final String SCHEDULE_RAN = "schedule.ran";
    /** {@code server.announcement} */
    public static final String SERVER_ANNOUNCEMENT = "server.announcement";
    /** {@code server.kickall} */
    public static final String SERVER_KICKALL = "server.kickall";
    /** {@code server.offline} */
    public static final String SERVER_OFFLINE = "server.offline";
    /** {@code server.online} */
    public static final String SERVER_ONLINE = "server.online";
    /** {@code server.resource} */
    public static final String SERVER_RESOURCE = "server.resource";
    /** {@code server.restart_scheduled} */
    public static final String SERVER_RESTART_SCHEDULED = "server.restart_scheduled";
    /** {@code server.restart_skipped} */
    public static final String SERVER_RESTART_SKIPPED = "server.restart_skipped";
    /** {@code server.restarted} */
    public static final String SERVER_RESTARTED = "server.restarted";
    /** {@code server.started} */
    public static final String SERVER_STARTED = "server.started";
    /** {@code server.stopped} */
    public static final String SERVER_STOPPED = "server.stopped";
    /** {@code txadmin.admin} */
    public static final String TXADMIN_ADMIN = "txadmin.admin";
    /** {@code whitelist.approval_added} */
    public static final String WHITELIST_APPROVAL_ADDED = "whitelist.approval_added";
    /** {@code whitelist.approval_removed} */
    public static final String WHITELIST_APPROVAL_REMOVED = "whitelist.approval_removed";
    /** {@code whitelist.approved} */
    public static final String WHITELIST_APPROVED = "whitelist.approved";
    /** {@code whitelist.denied} */
    public static final String WHITELIST_DENIED = "whitelist.denied";
    /** {@code whitelist.denied_all} */
    public static final String WHITELIST_DENIED_ALL = "whitelist.denied_all";

    /** Every event name above, sorted. */
    public static final List<String> ALL = List.of(
            ACTION_REVOKED,
            CONSOLE_COMMAND,
            LOG_ADMIN_ACTION,
            LOG_BAN,
            LOG_KILL,
            LOG_WARN,
            PING,
            PLAYER_BAN_SYNCED,
            PLAYER_BANNED,
            PLAYER_KICKED,
            PLAYER_MESSAGED,
            PLAYER_NOTE,
            PLAYER_WARNED,
            PLAYER_WHITELIST,
            QUOTA_WARNING,
            SCHEDULE_RAN,
            SERVER_ANNOUNCEMENT,
            SERVER_KICKALL,
            SERVER_OFFLINE,
            SERVER_ONLINE,
            SERVER_RESOURCE,
            SERVER_RESTART_SCHEDULED,
            SERVER_RESTART_SKIPPED,
            SERVER_RESTARTED,
            SERVER_STARTED,
            SERVER_STOPPED,
            TXADMIN_ADMIN,
            WHITELIST_APPROVAL_ADDED,
            WHITELIST_APPROVAL_REMOVED,
            WHITELIST_APPROVED,
            WHITELIST_DENIED,
            WHITELIST_DENIED_ALL);

    private WebhookEvents() {
    }
}
