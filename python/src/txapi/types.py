"""Typed response models.

Every model is a :class:`~typing.TypedDict` with ``total=False``: the values
returned by the SDK are plain ``dict`` objects, so fields the API adds in a
later minor version simply show up as extra keys and never break parsing.
Shapes follow the ``responseExample`` entries of the API catalog.
"""

from __future__ import annotations

from typing import Any, Literal, TypedDict, Union

__all__ = [
    "ActionLog",
    "ActionLogLine",
    "Activity",
    "ActivityBucket",
    "ActivityTotals",
    "Admin",
    "AdminCreated",
    "AdminList",
    "AdminPermission",
    "AdminSummary",
    "AvailablePermissions",
    "BanTemplate",
    "BanTemplates",
    "ConsoleBuffer",
    "DiscordGuildResult",
    "DiscordResult",
    "Event",
    "EventPage",
    "EventSummary",
    "HistoryPage",
    "HistoryRow",
    "JsonValue",
    "Leaderboard",
    "LeaderboardRow",
    "Me",
    "MeKey",
    "MePlan",
    "MeQuota",
    "MutationResult",
    "NamedCount",
    "PlayerEventStats",
    "PlayerPage",
    "PlayerRow",
    "PlayerStatsSummary",
    "Playtime",
    "PlaytimeRow",
    "ServerList",
    "ServerLogBuffer",
    "ServerLogEntry",
    "ServerLogHistory",
    "ServerStatus",
    "ServerSummary",
    "WebhookEvent",
    "WebhookEventName",
    "WhitelistApproval",
    "WhitelistApprovals",
]

JsonValue = Any
"""Untyped JSON passed through from txAdmin (``dict``, ``list``, ``str`` …)."""


# ── mutations ────────────────────────────────────────────────────────────────


class DiscordGuildResult(TypedDict, total=False):
    """Outcome of a Discord ban/unban on one linked Discord server."""

    guildId: str
    guildName: str
    ok: bool
    code: str
    """``banned``, ``unbanned``, ``not_banned``, ``disabled``, ``missing_permission``,
    ``hierarchy``, ``owner``, ``unknown_user``, ``invalid_user``, ``not_in_guild``,
    ``bot_unavailable``, ``failed`` …"""
    message: str


class DiscordResult(TypedDict, total=False):
    """``discord`` field of a ban (``discord_ban=True``) or revoke (``discord_unban=True``)."""

    discordId: str
    skipped: str
    """Set when nothing was attempted: ``no_discord_id``, ``no_linked_guild``, ``not_a_ban``."""
    message: str
    guilds: list[DiscordGuildResult]


class MutationResult(TypedDict, total=False):
    """Answer of every write endpoint: ``{success, message, discord?}``."""

    success: bool
    message: str
    discord: DiscordResult


# ── account ──────────────────────────────────────────────────────────────────


class MePlan(TypedDict, total=False):
    code: str
    features: list[str]
    serverLimit: int
    logRetentionDays: int


class MeQuota(TypedDict, total=False):
    unlimited: bool
    used: int
    limit: int
    remaining: int
    resetsAt: str


class MeKey(TypedDict, total=False):
    id: int
    name: str
    prefix: str
    serverId: int
    expiresAt: str
    scopes: list[str]
    allowedIps: list[str]


class Me(TypedDict, total=False):
    success: bool
    accountId: int
    plan: MePlan
    quota: MeQuota
    rpmLimit: int
    rpmUnlimited: bool
    auth: str
    """``apiKey`` or ``discordBot``."""
    key: MeKey


class ServerSummary(TypedDict, total=False):
    id: int
    name: str
    txServerName: str
    active: bool
    health: str
    """``ONLINE``, ``STOPPED`` or ``UNREACHABLE`` (only with offline alerts enabled)."""
    healthCheckedAt: str
    offlineAlerts: bool
    eventLog: bool
    banSync: bool


class ServerList(TypedDict, total=False):
    success: bool
    count: int
    boundServerId: int
    servers: list[ServerSummary]


# ── players ──────────────────────────────────────────────────────────────────


class PlayerRow(TypedDict, total=False):
    license: str
    displayName: str
    playTime: int
    """Minutes."""
    tsJoined: int
    """Unix seconds."""
    tsLastConnection: int
    """Unix seconds."""
    notes: str
    isAdmin: bool
    isOnline: bool
    isWhitelisted: bool


class PlayerPage(TypedDict, total=False):
    players: list[PlayerRow]
    hasReachedEnd: bool


# ── history ──────────────────────────────────────────────────────────────────


class HistoryRow(TypedDict, total=False):
    id: str
    type: str
    """``ban`` or ``warn``."""
    playerName: str
    author: str
    reason: str
    timestamp: int
    isRevoked: bool
    banExpiration: str
    """``permanent``, ``active`` or ``expired`` (bans only)."""
    warnAcked: bool
    """Warns only."""


class HistoryPage(TypedDict, total=False):
    history: list[HistoryRow]
    hasReachedEnd: bool


class BanTemplate(TypedDict, total=False):
    id: str
    reason: str
    duration: str | dict[str, Any]
    """``"permanent"`` or ``{"value": 2, "unit": "days"}``."""


BanTemplates = list[BanTemplate]


# ── server ───────────────────────────────────────────────────────────────────


class PlayerStatsSummary(TypedDict, total=False):
    total: int
    playedLast24h: int
    joinedLast24h: int
    joinedLast7d: int


class ServerStatus(TypedDict, total=False):
    serverName: str
    txaVersion: str
    fxsVersion: str
    online: bool
    """The FXServer process runs (absent when txAdmin's diagnostics are unreadable)."""
    txAdminReachable: bool
    playerStats: PlayerStatsSummary


class ConsoleBuffer(TypedDict, total=False):
    lines: list[str]
    count: int


ServerLogEntry = Union[str, dict[str, Any]]
"""A server-log entry: txAdmin's structured object, or a plain string."""


class ServerLogBuffer(TypedDict, total=False):
    entries: list[ServerLogEntry]
    count: int


class ServerLogHistory(TypedDict, total=False):
    boundry: bool
    """Spelled like txAdmin spells it."""
    log: list[ServerLogEntry]


# ── events ───────────────────────────────────────────────────────────────────


class Event(TypedDict, total=False):
    """A stored server event; which fields are present depends on ``type``."""

    id: int
    ts: str
    type: str
    """``join``, ``leave``, ``join_denied``, ``death``, ``chat``, ``command``, ``menu``,
    ``admin``, ``ban``, ``warn``, ``system``, ``other``."""
    source: str
    player: str
    playerRef: str
    victim: str
    victimRef: str
    killer: str
    killerRef: str
    cause: str
    pvp: bool
    actionId: str
    admin: str
    reason: str
    expiration: str
    revoked: bool
    message: str
    kind: str
    loggedTime: str
    session: str
    identifiers: list[str]


class EventPage(TypedDict, total=False):
    success: bool
    serverId: int
    eventLog: bool
    chatLog: bool
    count: int
    nextCursor: str
    """Absent on the last page."""
    events: list[Event]


class LeaderboardRow(TypedDict, total=False):
    rank: int
    player: str
    kills: int
    deaths: int
    kd: float


class Leaderboard(TypedDict, total=False):
    success: bool
    serverId: int
    eventLog: bool
    since: str
    sort: str
    players: list[LeaderboardRow]


class NamedCount(TypedDict, total=False):
    name: str
    count: int


class PlayerEventStats(TypedDict, total=False):
    success: bool
    serverId: int
    eventLog: bool
    player: str
    identifier: str
    names: list[str]
    since: str
    found: bool
    kills: int
    deaths: int
    pvpDeaths: int
    kd: float
    topVictims: list[NamedCount]
    topKillers: list[NamedCount]
    topWeapons: list[NamedCount]
    sessions: int
    firstSeen: str
    lastSeen: str
    bans: int
    warns: int
    recent: list[Event]


class EventSummary(TypedDict, total=False):
    success: bool
    serverId: int
    eventLog: bool
    since: str
    byType: dict[str, int]
    kills: int
    uniquePlayers: int
    topKiller: LeaderboardRow
    topCauses: list[NamedCount]
    oldestEvent: str
    newestEvent: str
    storedEvents: int


class PlaytimeRow(TypedDict, total=False):
    player: str
    identifier: str
    sessions: int
    seconds: int
    longestSeconds: int
    online: bool
    lastSeen: str


class Playtime(TypedDict, total=False):
    success: bool
    serverId: int
    eventLog: bool
    since: str
    totalSeconds: int
    sessionCount: int
    unclosedSessions: int
    players: list[PlaytimeRow]
    recentSessions: list[dict[str, Any]]


class ActivityTotals(TypedDict, total=False):
    joins: int
    deaths: int
    kills: int
    chat: int
    commands: int
    bans: int
    warns: int


class ActivityBucket(TypedDict, total=False):
    start: str
    joins: int
    players: int
    deaths: int
    kills: int
    chat: int
    commands: int
    bans: int
    warns: int


class Activity(TypedDict, total=False):
    success: bool
    serverId: int
    eventLog: bool
    since: str
    bucket: str
    timeZone: str
    totals: ActivityTotals
    buckets: list[ActivityBucket]


class ActionLogLine(TypedDict, total=False):
    time: str
    admin: str
    message: str
    kind: str


class ActionLog(TypedDict, total=False):
    lines: list[ActionLogLine]
    count: int
    total: int


# ── whitelist ────────────────────────────────────────────────────────────────


class WhitelistApproval(TypedDict, total=False):
    identifier: str
    playerName: str
    playerAvatar: str | None
    tsApproved: int
    approvedBy: str


WhitelistApprovals = list[WhitelistApproval]


# ── txAdmin panel admins ─────────────────────────────────────────────────────


class AdminSummary(TypedDict, total=False):
    name: str
    hasPassword: bool
    hasCitizenfx: bool
    hasDiscord: bool
    permsSummary: str
    isMaster: bool
    isSelf: bool


class AdminList(TypedDict, total=False):
    admins: list[AdminSummary]
    total: int


class Admin(TypedDict, total=False):
    name: str
    citizenfxID: str
    discordID: str
    permissions: list[str]


class AdminPermission(TypedDict, total=False):
    id: str
    label: str


class AvailablePermissions(TypedDict, total=False):
    permissions: list[AdminPermission]


class AdminCreated(TypedDict, total=False):
    success: bool
    password: str
    """One-time generated password — store it, it is never shown again."""
    note: str
    refresh: bool
    message: str


# ── webhooks ─────────────────────────────────────────────────────────────────


class WebhookEvent(TypedDict, total=False):
    """A verified webhook delivery (see :func:`txapi.verify_webhook`)."""

    id: str
    """Stable delivery id — use it to skip duplicates (deliveries can repeat)."""
    event: str
    createdAt: str
    serverId: int
    data: dict[str, Any]


WebhookEventName = Literal[
    "action.revoked",
    "console.command",
    "log.admin_action",
    "log.ban",
    "log.kill",
    "log.warn",
    "ping",
    "player.ban_synced",
    "player.banned",
    "player.kicked",
    "player.messaged",
    "player.note",
    "player.warned",
    "player.whitelist",
    "quota.warning",
    "schedule.ran",
    "server.announcement",
    "server.kickall",
    "server.offline",
    "server.online",
    "server.resource",
    "server.restart_scheduled",
    "server.restart_skipped",
    "server.restarted",
    "server.started",
    "server.stopped",
    "txadmin.admin",
    "whitelist.approval_added",
    "whitelist.approval_removed",
    "whitelist.approved",
    "whitelist.denied",
    "whitelist.denied_all",
]
