#!/usr/bin/env python3
"""Generate the endpoint methods of the txApi Python SDK.

Reads ``../../spec/endpoints.json`` (generated from the API catalog) plus the
Python-specific naming/documentation table below and writes

* ``src/txapi/_endpoints.py`` — one :class:`EndpointSpec` per endpoint, and
* ``src/txapi/_generated.py`` — the resource classes for :class:`TxApi`,
  :class:`AsyncTxApi` and their ``with_raw_response`` views.

Run from ``sdks/python``::

    python scripts/generate.py           # write the files
    python scripts/generate.py --check   # fail if they are out of date

The output is piped through ``ruff format`` so it matches the formatter.
Adding an endpoint to the API without an entry in ``METHODS`` fails here.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
import textwrap
from dataclasses import dataclass
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
SPEC = ROOT.parent / "spec" / "endpoints.json"
OUT_ENDPOINTS = ROOT / "src" / "txapi" / "_endpoints.py"
OUT_GENERATED = ROOT / "src" / "txapi" / "_generated.py"


@dataclass(frozen=True)
class M:
    group: tuple[str, ...]
    name: str
    returns: str
    summary: str
    details: str = ""
    paging: str | None = None  # "players" | "history" | "events"


MUTATION = "t.MutationResult"
ANY = "t.JsonValue"

METHODS: dict[str, M] = {
    # ── account
    "account.me": M(
        ("account",),
        "me",
        "t.Me",
        "Return the calling account: plan, effective features, monthly quota, rate limit and key.",
        "Effective features are the plan's features intersected with the key's scopes — exactly what "
        "the permission check uses. Over the Discord bot ``auth`` is ``discordBot`` and ``key`` is "
        "absent. Counts against the monthly quota like every call, but works even when it is used up.",
    ),
    "account.servers": M(
        ("account",),
        "servers",
        "t.ServerList",
        "List the servers reachable with this key (values for ``server_id``).",
        "A server-bound key only sees its own server (``boundServerId``). No URLs, no credentials.",
    ),
    # ── players
    "players.search": M(
        ("players",),
        "search",
        "t.PlayerPage",
        "Search players by display name (one page of at most 100 rows).",
        "Without ``query`` the complete, sorted player list. Use :meth:`iter_search` to page "
        "through all results automatically.",
        "players",
    ),
    "players.online": M(
        ("players",),
        "online",
        "t.PlayerPage",
        "Players currently connected, by play time (one page).",
        "",
        "players",
    ),
    "players.lookup": M(
        ("players",),
        "lookup",
        "t.PlayerPage",
        "Find players by identifier (license / discord / fivem / steam), one page.",
        "The identifier prefix is detected automatically; raw and prefixed values both work.",
        "players",
    ),
    "players.info": M(
        ("players",), "info", ANY, "Return the full player record as txAdmin delivers it."
    ),
    "players.stats": M(
        ("players",), "stats", ANY, "Return player-database statistics (totals, last 24 h …)."
    ),
    "players.warn": M(("players",), "warn", MUTATION, "Warn a player."),
    "players.ban": M(
        ("players",),
        "ban",
        MUTATION,
        "Ban a player.",
        "Omit ``duration_hours`` (or pass 0) for a permanent ban. With ``discord_ban=True`` the "
        "player is additionally banned on every Discord server whose txBot is linked to this "
        "txAdmin server; the result is in ``discord`` of the answer (a Discord ban is always "
        "permanent). The txAdmin ban applies regardless of the Discord outcome.",
    ),
    "players.kick": M(("players",), "kick", MUTATION, "Kick a player from the server."),
    "players.dm": M(("players",), "dm", MUTATION, "Send a direct message to a player."),
    "players.notes": M(
        ("players",),
        "notes",
        MUTATION,
        "Set the staff note of a player (an empty note deletes it).",
    ),
    "players.whitelist": M(
        ("players",),
        "whitelist",
        MUTATION,
        "Add a player to or remove them from the whitelist.",
        "``whitelisted`` is mandatory on purpose: a missing value never silently removes a player.",
    ),
    # ── history
    "history.bans": M(
        ("history",),
        "bans",
        "t.HistoryPage",
        "List bans, newest first by default (one page of at most 100 rows).",
        "",
        "history",
    ),
    "history.warns": M(
        ("history",),
        "warns",
        "t.HistoryPage",
        "List warnings (one page of at most 100 rows).",
        "",
        "history",
    ),
    "history.search": M(
        ("history",),
        "search",
        "t.HistoryPage",
        "Filter and search bans and warnings (one page).",
        "``search`` without ``search_type`` searches the reason.",
        "history",
    ),
    "history.ban-templates": M(
        ("history",),
        "ban_templates",
        "t.BanTemplates",
        "Return the ban templates configured in txAdmin (reason + duration).",
    ),
    "history.action": M(("history",), "action", ANY, "Return a single ban or warning by id."),
    "history.stats": M(("history",), "stats", ANY, "Return the number of bans and warnings."),
    "history.revoke": M(
        ("history",),
        "revoke",
        MUTATION,
        "Revoke a ban or warning.",
        "With ``discord_unban=True`` a ban is also lifted on the linked Discord servers; the "
        "result is in ``discord`` of the answer (``skipped: not_a_ban`` for a warning).",
    ),
    # ── server
    "server.status": M(
        ("server",),
        "status",
        "t.ServerStatus",
        "Return the server status: FXServer running, versions, player-database stats.",
        "``online`` means the FXServer process runs; ``txAdminReachable`` means the panel answers.",
    ),
    "server.diagnostics": M(
        ("server",),
        "diagnostics",
        ANY,
        "Return txAdmin's diagnostics (uptime, host, memory, processes).",
    ),
    "server.drops": M(
        ("server",), "drops", ANY, "Return player-drop statistics for crash analysis."
    ),
    "server.performance": M(
        ("server",),
        "performance",
        ANY,
        "Return the FXServer thread performance histogram (tick times).",
    ),
    "server.restart": M(("server",), "restart", MUTATION, "Restart the FXServer."),
    "server.start": M(("server",), "start", MUTATION, "Start the FXServer."),
    "server.stop": M(("server",), "stop", MUTATION, "Stop the FXServer."),
    "server.schedule-restart": M(
        ("server",),
        "schedule_restart",
        MUTATION,
        "Schedule a one-off restart through txAdmin's scheduler.",
        "Players get txAdmin's usual restart warnings.",
    ),
    "server.skip-restart": M(
        ("server",), "skip_restart", MUTATION, "Skip (or un-skip) the next scheduled restart."
    ),
    "server.kickall": M(
        ("server",), "kick_all", MUTATION, "Kick every player (e.g. for maintenance)."
    ),
    "server.announcement": M(
        ("server",), "announcement", MUTATION, "Broadcast a server announcement."
    ),
    "server.resource.start": M(("server", "resources"), "start", MUTATION, "Start a resource."),
    "server.resource.stop": M(("server", "resources"), "stop", MUTATION, "Stop a resource."),
    "server.resource.restart": M(
        ("server", "resources"), "restart", MUTATION, "Restart a resource."
    ),
    "server.resource.ensure": M(
        ("server", "resources"), "ensure", MUTATION, "Start a resource if it is stopped."
    ),
    "server.resource.refresh": M(
        ("server", "resources"), "refresh", MUTATION, "Rescan the resources directory."
    ),
    # ── console
    "console.buffer": M(
        ("console",),
        "buffer",
        "t.ConsoleBuffer",
        "Return the last 500 console lines (with FXServer's ANSI colour codes).",
    ),
    "console.command": M(
        ("console",),
        "command",
        MUTATION,
        "Run a console command.",
        "Fire-and-forget: success only means txAdmin accepted the command. Read the output via "
        ":meth:`buffer` or the live stream (:meth:`connect`).",
    ),
    # ── server log
    "serverlog.buffer": M(
        ("serverlog",),
        "buffer",
        "t.ServerLogBuffer",
        "Return the last 500 server-log entries (objects or strings).",
    ),
    "serverlog.history": M(
        ("serverlog",),
        "history",
        "t.ServerLogHistory",
        "Return older server-log pages live from txAdmin (up to 500 entries).",
        "To page backwards pass ``ref`` = ``ts`` of the oldest entry received.",
    ),
    # ── events
    "events.list": M(
        ("events",),
        "list",
        "t.EventPage",
        "List stored events, newest first (one page).",
        "Requires the event log to be enabled for the server in the dashboard.",
        "events",
    ),
    "events.deaths": M(
        ("events",),
        "deaths",
        "t.EventPage",
        "List deaths with cause and killer (one page).",
        "",
        "events",
    ),
    "events.kills": M(
        ("events",),
        "kills",
        "t.EventPage",
        "List kills, i.e. deaths caused by players (one page).",
        "",
        "events",
    ),
    "events.leaderboard": M(
        ("events",), "leaderboard", "t.Leaderboard", "Return the kills / deaths / K/D leaderboard."
    ),
    "events.player": M(
        ("events",),
        "player",
        "t.PlayerEventStats",
        "Return the statistics of one player.",
        "Unknown player: ``found`` is ``False`` (no 404).",
    ),
    "events.summary": M(
        ("events",),
        "summary",
        "t.EventSummary",
        "Return a summary: events per type, kills, unique players.",
    ),
    "events.chat": M(
        ("events",),
        "chat",
        "t.EventPage",
        "List chat messages (one page; only when chat logging is enabled).",
        "Check ``chatLog`` in the answer: with ``False`` an empty list does not mean nobody wrote.",
        "events",
    ),
    "events.commands": M(
        ("events",),
        "commands",
        "t.EventPage",
        "List commands executed by players (one page).",
        "",
        "events",
    ),
    "events.admin-actions": M(
        ("events",),
        "admin_actions",
        "t.EventPage",
        "List admin actions from the txAdmin panel and in-game menu (one page).",
        "",
        "events",
    ),
    "events.actions": M(
        ("events",),
        "actions",
        "t.EventPage",
        "List bans and warnings from every source (one page).",
        "",
        "events",
    ),
    "events.sessions": M(
        ("events",),
        "sessions",
        "t.EventPage",
        "List joins and leaves with reason (one page).",
        "",
        "events",
    ),
    "events.playtime": M(("events",), "playtime", "t.Playtime", "Return the play time per player."),
    "events.activity": M(
        ("events",),
        "activity",
        "t.Activity",
        "Return activity per hour or day (joins, players, deaths, kills, chat …).",
    ),
    "events.action-log": M(
        ("events",),
        "action_log",
        "t.ActionLog",
        "Return txAdmin's current admin log live (not stored).",
        "The txAdmin account needs the ``txadmin.log.view`` permission.",
    ),
    # ── whitelist
    "whitelist.requests": M(("whitelist",), "requests", ANY, "List open whitelist requests."),
    "whitelist.approve": M(("whitelist",), "approve", MUTATION, "Approve a whitelist request."),
    "whitelist.deny": M(
        ("whitelist",),
        "deny",
        MUTATION,
        "Deny a whitelist request.",
        "txAdmin confirms denying even an unknown request id.",
    ),
    "whitelist.deny-all": M(
        ("whitelist",),
        "deny_all",
        MUTATION,
        "Deny every open request whose last attempt was at or before ``before``.",
        "Without ``before`` every request up to now — requests arriving after you loaded the "
        "list stay.",
    ),
    "whitelist.approvals": M(
        ("whitelist", "approvals"),
        "list",
        "t.WhitelistApprovals",
        "List identifiers approved in advance.",
    ),
    "whitelist.approvals.add": M(
        ("whitelist", "approvals"),
        "add",
        MUTATION,
        "Approve an identifier in advance.",
        "Typed identifiers only (``license:``, ``discord:``, ``fivem:``, ``steam:`` …), never an IP.",
    ),
    "whitelist.approvals.remove": M(
        ("whitelist", "approvals"), "remove", MUTATION, "Remove an advance approval."
    ),
    # ── admins
    "admins.list": M(("admins",), "list", "t.AdminList", "List the txAdmin panel admins."),
    "admins.get": M(
        ("admins",),
        "get",
        "t.Admin",
        "Return one panel admin with all permission keys.",
        "The master account is not returned by txAdmin (403).",
    ),
    "admins.permissions": M(
        ("admins",),
        "permissions",
        "t.AvailablePermissions",
        "List every permission key that can be assigned to an admin.",
    ),
    "admins.add": M(
        ("admins",),
        "add",
        "t.AdminCreated",
        "Create a panel admin.",
        "The answer contains a one-time generated ``password`` — store it, it is never shown "
        "again.",
    ),
    "admins.edit": M(
        ("admins",),
        "edit",
        MUTATION,
        "Update a panel admin.",
        "Only the fields you pass change; omitted fields keep their current value.",
    ),
    "admins.delete": M(("admins",), "delete", MUTATION, "Delete a panel admin."),
}

PARAM_DOCS: dict[str, str] = {
    "identifier": "Player identifier (``license:…``, ``discord:…``, ``fivem:…``, ``steam:…``).",
    "reason": "Reason shown to the player / stored in the audit log.",
    "durationHours": "Hours until the ban expires; omit or 0 for a permanent ban.",
    "discordBan": "Also ban on every linked Discord server (needs txBot; permanent there).",
    "discordUnban": "Also lift the ban on the linked Discord servers (needs txBot).",
    "message": "Message text.",
    "note": "Note text; empty deletes the note.",
    "whitelisted": "``True`` adds the player, ``False`` removes them.",
    "query": "Part of the display name; omit to list every player.",
    "sortBy": "``playTime`` (default), ``tsJoined`` or ``tsLastConnection``.",
    "order": "``asc`` or ``desc`` (default ``desc``).",
    "filters": "Quick filters ``isOnline``, ``isAdmin``, ``isWhitelisted``, ``hasNote`` "
    "(comma-separated string or sequence).",
    "type": "Type filter.",
    "admin": "Name of the txAdmin admin.",
    "searchType": "``actionId``, ``reason`` or ``identifiers`` (default ``reason``).",
    "search": "Search text.",
    "actionId": "Ban / warning id.",
    "thread": "``svMain`` (default), ``svNetwork`` or ``svSync``.",
    "time": "``HH:MM`` (24 h, txAdmin host time) or ``+N`` minutes (1–1439).",
    "skip": "``True`` skips the next scheduled restart, ``False`` restores it.",
    "name": "Name.",
    "command": "FXServer / txAdmin console command.",
    "direction": "``older`` (default) or ``newer``.",
    "ref": "Reference time in epoch milliseconds (default now).",
    "player": "Player name (exact, case-insensitive) or identifier (``license:…``).",
    "since": "Start: ISO-8601, epoch seconds/milliseconds or relative (``30m``, ``24h``, ``7d``).",
    "until": "End (exclusive), same formats as ``since``.",
    "limit": "Page size 1–500 (default 100).",
    "victim": "Victim (name or identifier).",
    "killer": "Killer (name or identifier).",
    "cause": "Cause / weapon, exact.",
    "pvp": "``True`` = only deaths by players, ``False`` = only without killer.",
    "sort": "``kills`` (default), ``deaths`` or ``kd``.",
    "kind": "Rough kind: ban, warn, kick, revoke, command, restart, whitelist, auth, other.",
    "revoked": "``True`` = only revoked, ``False`` = only active.",
    "bucket": "``hour`` (default) or ``day``.",
    "timeZone": "IANA time zone for the bucket boundaries (default UTC).",
    "lines": "Last N lines, 1–2000 (default 200).",
    "page": "1-based page.",
    "requestId": "Whitelist request id.",
    "before": "Unix time in seconds (milliseconds are detected); default now.",
    "citizenfxId": "CitizenFX forum id.",
    "discordId": "Discord user id (for OAuth login).",
    "permissions": "txAdmin permission keys, at least one (see :meth:`permissions`).",
}

PARAM_OVERRIDES: dict[tuple[str, str], str] = {
    ("history.search", "type"): "``ban`` or ``warn``; omit for both.",
    ("events.list", "type"): "Comma-separated string or sequence of event types: join, leave, "
    "join_denied, death, chat, command, menu, admin, ban, warn, system, other.",
    ("events.actions", "type"): "``ban`` or ``warn`` (default both).",
    ("events.actions", "player"): "Affected player.",
    ("events.actions", "admin"): "Admin who issued the action.",
    ("events.deaths", "player"): "Involved as victim or killer.",
    ("events.kills", "player"): "Involved as victim or killer.",
    ("events.leaderboard", "since"): "Period, default ``7d``.",
    ("events.leaderboard", "limit"): "1–100, default 10.",
    ("events.player", "since"): "Period for kills/deaths/sessions, default ``30d``.",
    ("events.summary", "since"): "Period, default ``24h``.",
    ("events.playtime", "since"): "Period, default ``7d``.",
    ("events.playtime", "limit"): "Number of players, 1–100, default 10.",
    ("events.activity", "since"): "Period, default ``24h``.",
    ("server.resource.start", "name"): "Resource (folder) name: letters, digits, ``_ - .``.",
    ("server.resource.stop", "name"): "Resource (folder) name: letters, digits, ``_ - .``.",
    ("server.resource.restart", "name"): "Resource (folder) name: letters, digits, ``_ - .``.",
    ("server.resource.ensure", "name"): "Resource (folder) name: letters, digits, ``_ - .``.",
    ("admins.get", "name"): "Admin login name.",
    ("admins.add", "name"): "Login name.",
    ("admins.edit", "name"): "Login name.",
    ("admins.delete", "name"): "Login name.",
    ("admins.edit", "permissions"): "Replaces the current permission keys (at least one).",
    ("whitelist.approvals.add", "identifier"): "Identifier, e.g. ``discord:<id>``.",
    ("whitelist.approvals.remove", "identifier"): "The approved identifier.",
    ("server.kickall", "reason"): "Reason shown to the players.",
    ("history.revoke", "reason"): "Audit reason.",
}

CURSOR_DOCS = {
    "players": {
        "offset": "Cursor: sort-key value of the last row of the previous page (with ``cursor``).",
        "cursor": "Cursor: ``license`` of the last row of the previous page (with ``offset``).",
    },
    "history": {
        "offset": "Cursor: ``timestamp`` of the last row of the previous page (with ``cursor``).",
        "cursor": "Cursor: ``id`` of the last row of the previous page (with ``offset``).",
    },
    "events": {"cursor": "``nextCursor`` of the previous page."},
}

CSV_PARAMS = {("players.search", "filters"), ("events.list", "type")}

ITEM_TYPES = {"players": "t.PlayerRow", "history": "t.HistoryRow", "events": "t.Event"}
PAGE_CURSOR_PARAMS = {
    "players": ("offset", "cursor"),
    "history": ("offset", "cursor"),
    "events": ("cursor",),
}

GROUP_DOCS = {
    ("account",): "Account-level calls (no server involved).",
    ("players",): "Players: search, look up, warn, ban, kick, message, notes, whitelist.",
    ("history",): "Ban and warning history.",
    ("server",): "Server control: status, restart, scheduling, announcements, resources.",
    ("server", "resources"): "Start, stop, restart and ensure single resources.",
    ("console",): "Live console: buffer, commands and the live stream.",
    ("serverlog",): "Server log (joins, leaves, deaths, chat): buffer, history, live stream.",
    ("events",): "Stored event log: deaths, kills, chat, admin actions, statistics.",
    ("whitelist",): "Whitelist requests and advance approvals.",
    ("whitelist", "approvals"): "Identifiers approved in advance.",
    ("admins",): "txAdmin panel admin accounts.",
}

ROOT_ORDER = [
    "account",
    "players",
    "history",
    "server",
    "console",
    "serverlog",
    "events",
    "whitelist",
    "admins",
]


def snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def camel_class(group: tuple[str, ...]) -> str:
    names = {"serverlog": "ServerLog"}
    return "".join(names.get(g, g[:1].upper() + g[1:]) for g in group)


PY_TYPES = {"string": "str", "integer": "int", "boolean": "bool", "string[]": "Sequence[str]"}


@dataclass(frozen=True)
class Arg:
    wire: str
    py: str
    kind: str  # "path" | "query" | "body"
    required: bool
    py_type: str
    doc: str


def args_for(ep: dict, meta: M) -> list[Arg]:
    out: list[Arg] = []
    for p in ep["params"]:
        loc = "path" if "{" + p["name"] + "}" in ep["path"] else "query"
        typ = PY_TYPES[p["type"]]
        if (ep["id"], p["name"]) in CSV_PARAMS:
            typ = "str | Sequence[str]"
        doc = (
            PARAM_OVERRIDES.get((ep["id"], p["name"]))
            or (CURSOR_DOCS.get(meta.paging or "", {}).get(p["name"]))
            or PARAM_DOCS[p["name"]]
        )
        out.append(Arg(p["name"], snake(p["name"]), loc, bool(p["required"]), typ, doc))
    for b in ep["body"]:
        doc = PARAM_OVERRIDES.get((ep["id"], b["name"])) or PARAM_DOCS[b["name"]]
        if b.get("maxLength"):
            doc += f" At most {b['maxLength']} characters."
        out.append(
            Arg(b["name"], snake(b["name"]), "body", bool(b["required"]), PY_TYPES[b["type"]], doc)
        )
    return out


def signature(ep: dict, args: list[Arg], *, drop: tuple[str, ...] = ()) -> list[str]:
    parts = ["self"]
    path_args = [a for a in args if a.kind == "path"]
    for a in path_args:
        parts.append(f"{a.py}: {a.py_type}")
    parts.append("*")
    others = [a for a in args if a.kind != "path" and a.wire not in drop]
    for a in [a for a in others if a.required]:
        parts.append(f"{a.py}: {a.py_type}")
    for a in [a for a in others if not a.required]:
        parts.append(f"{a.py}: {a.py_type} | None = None")
    if not ep["accountLevel"]:
        parts.append("server_id: int | str | None = None")
    parts.append("timeout: float | None = None")
    return parts


def docstring(ep: dict, meta: M, args: list[Arg], *, kind: str, drop: tuple[str, ...] = ()) -> str:
    lines: list[str] = []
    if kind == "iter":
        item = {"players": "player rows", "history": "history rows", "events": "events"}[
            meta.paging or ""
        ]
        lines.append(f"Iterate over all {item} of :meth:`{meta.name}`, fetching pages lazily.")
        lines.append("")
        lines.append(
            "Stops when the API reports the end, the cursor is missing or a page is empty."
        )
    else:
        lines.append(meta.summary)
        if meta.details:
            lines.append("")
            lines.append(meta.details)
    lines.append("")
    perm = ep["permission"]
    lines.append(f"Permission: ``{perm}``." if perm else "Permission: none (any valid key).")
    lines.append(f"Endpoint: ``{ep['method']} {ep['path']}``.")
    shown = [a for a in args if a.wire not in drop]
    lines.append("")
    lines.append("Args:")
    for a in shown:
        lines.append(f"    {a.py}: {a.doc}")
    if not ep["accountLevel"]:
        lines.append("    server_id: Server to address; defaults to the client's ``server_id``.")
    lines.append("    timeout: Per-attempt timeout in seconds; defaults to the client's.")
    return lines


def call_args(ep: dict, args: list[Arg]) -> str:
    def mapping(kind: str) -> str:
        items = [f'"{a.wire}": {a.py}' for a in args if a.kind == kind]
        return "{" + ", ".join(items) + "}"

    body = mapping("body") if ep["body"] else "None"
    server = "server_id" if not ep["accountLevel"] else "None"
    return (
        f'_E["{ep["id"]}"], path={mapping("path")}, query={mapping("query")}, body={body}, '
        f"server_id={server}, timeout=timeout"
    )


def indent(lines: list[str], n: int) -> list[str]:
    pad = " " * n
    return [pad + line if line else "" for line in lines]


def wrap_doc(lines: list[str], width: int) -> list[str]:
    out: list[str] = []
    for line in lines:
        if not line:
            out.append("")
        elif line.startswith("    "):
            out += textwrap.wrap(
                line.strip(),
                width,
                initial_indent="    ",
                subsequent_indent="        ",
                break_on_hyphens=False,
            )
        else:
            out += textwrap.wrap(line, width, break_on_hyphens=False)
    return out


def render_doc(lines: list[str], n: int) -> list[str]:
    # n is the indent of the docstring; methods end up 4 deeper inside the class.
    lines = wrap_doc(lines, 100 - n - 4 - 3)
    out = ['"""' + lines[0], *lines[1:], '"""']
    return indent(out, n)


VARIANTS = {
    # key: (class prefix, class suffix, is_async, is_raw)
    "sync": ("", "", False, False),
    "async": ("Async", "", True, False),
    "sync_raw": ("", "WithRawResponse", False, True),
    "async_raw": ("Async", "WithRawResponse", True, True),
}

REQUESTER = {False: "SyncRequester", True: "AsyncRequester"}


def class_name(group: tuple[str, ...], variant: str) -> str:
    prefix, suffix, _, _ = VARIANTS[variant]
    return prefix + camel_class(group) + suffix


def method_lines(ep: dict, meta: M, variant: str) -> list[str]:
    _, _, is_async, is_raw = VARIANTS[variant]
    args = args_for(ep, meta)
    ret = f"ApiResponse[{meta.returns}]" if is_raw else meta.returns
    sig = signature(ep, args)
    head = (
        ("async def " if is_async else "def ") + f"{meta.name}(" + ", ".join(sig) + f") -> {ret}:"
    )
    out = [head]
    out += render_doc(docstring(ep, meta, args, kind="call"), 4)
    call = f"self._client._request({call_args(ep, args)})"
    if is_async:
        call = f"(await {call})"
    if is_raw and meta.returns == ANY:
        out.append(f"    return {call}")
    elif is_raw:
        out.append(f'    return cast("{ret}", {call})')
    else:
        out.append(f'    return cast("{ret}", {call}.data)')
    out.append("")
    return out


def iter_lines(ep: dict, meta: M, variant: str) -> list[str]:
    _, _, is_async, _ = VARIANTS[variant]
    paging = meta.paging or ""
    args = args_for(ep, meta)
    drop = PAGE_CURSOR_PARAMS[paging]
    item = ITEM_TYPES[paging]
    page_type = meta.returns
    ret = f"AsyncIterator[{item}]" if is_async else f"Iterator[{item}]"
    sig = signature(ep, args, drop=drop)
    out = [f"def iter_{meta.name}(" + ", ".join(sig) + f") -> {ret}:"]
    out += render_doc(docstring(ep, meta, args, kind="iter", drop=drop), 4)
    kept = [a for a in args if a.kind != "path" and a.wire not in drop]
    passed = [f"{a.py}={a.py}" for a in kept]
    if paging in ("players", "history"):
        params = "offset: int | None, cursor: str | None"
        passed += ["offset=offset", "cursor=cursor"]
    else:
        params = "cursor: str | None"
        passed += ["cursor=cursor"]
    passed += ["server_id=server_id", "timeout=timeout"]
    call = f"self.{meta.name}(" + ", ".join(passed) + ")"
    if is_async:
        out.append(f"    async def fetch({params}) -> {page_type}:")
        out.append(f"        return await {call}")
    else:
        out.append(f"    def fetch({params}) -> {page_type}:")
        out.append(f"        return {call}")
    helper = {"players": "players", "history": "history", "events": "events"}[paging]
    fn = ("_pagination.aiter_" if is_async else "_pagination.iter_") + helper
    if paging == "players":
        key = (
            "_pagination.player_sort_key(sort_by)"
            if any(a.py == "sort_by" for a in kept)
            else '"playTime"'
        )
        out.append(f"    return {fn}(fetch, {key})")
    else:
        out.append(f"    return {fn}(fetch)")
    out.append("")
    return out


CONNECT = {
    "console": (
        "ConsoleStream",
        "t_live.ConsoleEvent",
        "console.write",
        "/ws/console",
        "Open the live console stream (``/ws/console``).",
        "Yields :data:`txapi.live.ConsoleEvent` values and offers "
        ":meth:`~txapi.live.ConsoleStream.send_command`.",
    ),
    "serverlog": (
        "ServerLogStream",
        "t_live.ServerLogEvent",
        "logs.view",
        "/ws/serverlog",
        "Open the live server-log stream (``/ws/serverlog``, read-only).",
        "Yields :data:`txapi.live.ServerLogEvent` values.",
    ),
}


def connect_lines(group: str) -> list[str]:
    cls, event, perm, path, summary, details = CONNECT[group]
    extra = ", command_timeout: float = 10.0" if group == "console" else ""
    head = (
        f"def connect(self, *, server_id: int | str | None = None, raw_frames: bool = False, "
        f"on_event: Callable[[{event}], Any] | None = None, reconnect: bool = True, "
        f"max_reconnect_attempts: int | None = None, initial_reconnect_delay: float = 1.0, "
        f"max_reconnect_delay: float = 30.0, open_timeout: float = 10.0{extra}"
        f") -> t_live.{cls}:"
    )
    doc = [
        summary,
        "",
        details,
        "",
        "Use it inside a running event loop: ``async with client.console.connect() as s:``.",
        'Needs the optional ``websockets`` dependency (``pip install "txapi-sdk[live]"``).',
        "",
        f"Permission: ``{perm}``. Endpoint: ``WS {path}``.",
        "",
        "Args:",
        "    server_id: Server to stream; defaults to the client's ``server_id``.",
        "    raw_frames: Also emit every raw frame as :class:`~txapi.live.FrameEvent`.",
        "    on_event: Callback (sync or async) called for every event.",
        "    reconnect: Reconnect after 1001/1006/1011/1012 and network loss (never 1008/1009).",
        "    max_reconnect_attempts: Give up after this many attempts in a row (``None`` = never).",
        "    initial_reconnect_delay: First backoff delay in seconds (doubles, with jitter).",
        "    max_reconnect_delay: Backoff ceiling in seconds.",
        "    open_timeout: Handshake timeout in seconds.",
    ]
    if group == "console":
        doc.append("    command_timeout: How long ``send_command`` waits for the ack.")
    kwargs = (
        "raw_frames=raw_frames, on_event=on_event, reconnect=reconnect, "
        "max_reconnect_attempts=max_reconnect_attempts, "
        "initial_reconnect_delay=initial_reconnect_delay, "
        "max_reconnect_delay=max_reconnect_delay, open_timeout=open_timeout"
    )
    if group == "console":
        kwargs += ", command_timeout=command_timeout"
    out = [head, *render_doc(doc, 4)]
    out.append(f"    return t_live.{cls}(self._client._live_config(server_id), {kwargs})")
    out.append("")
    return out


def build_tree(endpoints: list[dict]) -> dict[tuple[str, ...], list[dict]]:
    tree: dict[tuple[str, ...], list[dict]] = {}
    for ep in endpoints:
        meta = METHODS[ep["id"]]
        tree.setdefault(meta.group, []).append(ep)
    return tree


def children(group: tuple[str, ...], groups: list[tuple[str, ...]]) -> list[tuple[str, ...]]:
    return [g for g in groups if len(g) == len(group) + 1 and g[: len(group)] == group]


def render_class(
    group: tuple[str, ...], eps: list[dict], groups: list[tuple[str, ...]], variant: str
) -> list[str]:
    _, _, is_async, is_raw = VARIANTS[variant]
    name = class_name(group, variant)
    out = [f"class {name}:"]
    doc = GROUP_DOCS[group]
    if is_raw:
        doc += " Methods return :class:`~txapi.ApiResponse` (data + status, headers, rate limit)."
    out += render_doc([doc], 4)
    out.append("")
    kids = children(group, groups)
    for kid in kids:
        out.append(f"    {kid[-1]}: {class_name(kid, variant)}")
    if kids:
        out.append("")
    out.append(f"    def __init__(self, client: {REQUESTER[is_async]}) -> None:")
    out.append("        self._client = client")
    for kid in kids:
        out.append(f"        self.{kid[-1]} = {class_name(kid, variant)}(client)")
    out.append("")
    for ep in eps:
        meta = METHODS[ep["id"]]
        out += indent(method_lines(ep, meta, variant), 4)
        if meta.paging and not is_raw:
            out += indent(iter_lines(ep, meta, variant), 4)
    if not is_raw and group in (("console",), ("serverlog",)):
        out += indent(connect_lines(group[0]), 4)
    out.append("")
    return out


def render_root(variant: str) -> list[str]:
    _, _, is_async, is_raw = VARIANTS[variant]
    name = {
        "sync": "SyncGroups",
        "async": "AsyncGroups",
        "sync_raw": "TxApiWithRawResponse",
        "async_raw": "AsyncTxApiWithRawResponse",
    }[variant]
    doc = (
        "Endpoint groups of the client."
        if not is_raw
        else "Same methods as the client, returning :class:`~txapi.ApiResponse` objects."
    )
    out = [f"class {name}:", *render_doc([doc], 4), ""]
    for g in ROOT_ORDER:
        out.append(f"    {g}: {class_name((g,), variant)}")
        out += render_doc([GROUP_DOCS[(g,)]], 4)
    out.append("")
    out.append(f"    def __init__(self, client: {REQUESTER[is_async]}) -> None:")
    for g in ROOT_ORDER:
        out.append(f"        self.{g} = {class_name((g,), variant)}(client)")
    out.append("")
    out.append("")
    return out


HEADER_GENERATED = '''"""Endpoint methods — GENERATED by ``scripts/generate.py`` from ``spec/endpoints.json``.

Do not edit by hand: change the generator (or the API catalog) and regenerate.
"""

from __future__ import annotations

from collections.abc import AsyncIterator, Callable, Iterator, Sequence
from typing import Any, cast

from . import _pagination
from . import live as t_live
from . import types as t
from ._core import ApiResponse
from ._endpoints import ENDPOINTS as _E
from ._protocols import AsyncRequester, SyncRequester

__all__ = [
{all_names}
]


'''

HEADER_ENDPOINTS = '''"""Endpoint descriptors — GENERATED by ``scripts/generate.py`` from ``spec/endpoints.json``.

Do not edit by hand.
"""

from __future__ import annotations

from ._core import EndpointSpec, FieldSpec

API_VERSION = "{api_version}"

ENDPOINTS: dict[str, EndpointSpec] = {{
'''


def field_spec(name: str, required: bool, typ: str, max_length: int | None) -> str:
    extra = f", max_length={max_length}" if max_length else ""
    return f'FieldSpec("{name}", "{snake(name)}", required={required}, type="{typ}"{extra})'


def render_endpoints(spec: dict) -> str:
    out = [HEADER_ENDPOINTS.format(api_version=spec["apiVersion"])]
    for ep in spec["endpoints"]:
        path_params = [p for p in ep["params"] if "{" + p["name"] + "}" in ep["path"]]
        query = [p for p in ep["params"] if p not in path_params]
        pp = ", ".join(field_spec(p["name"], True, p["type"], None) for p in path_params)
        qq = ", ".join(field_spec(p["name"], bool(p["required"]), p["type"], None) for p in query)
        bb = ", ".join(
            field_spec(b["name"], bool(b["required"]), b["type"], b.get("maxLength"))
            for b in ep["body"]
        )
        perm = f'"{ep["permission"]}"' if ep["permission"] else "None"
        out.append(
            f'    "{ep["id"]}": EndpointSpec("{ep["id"]}", "{ep["method"]}", "{ep["path"]}", '
            f"{perm}, {bool(ep['accountLevel'])}, path_params=({pp}{',' if pp else ''}), "
            f"query=({qq}{',' if qq else ''}), body=({bb}{',' if bb else ''})),\n"
        )
    out.append("}\n")
    return "".join(out)


def render_generated(spec: dict) -> str:
    endpoints = spec["endpoints"]
    missing = [e["id"] for e in endpoints if e["id"] not in METHODS]
    extra = sorted(set(METHODS) - {e["id"] for e in endpoints})
    if missing or extra:
        raise SystemExit(
            f"METHODS out of sync with endpoints.json: missing={missing} extra={extra}"
        )
    tree = build_tree(endpoints)
    groups = sorted(tree, key=lambda g: (ROOT_ORDER.index(g[0]), len(g)))
    body: list[str] = []
    names: list[str] = []
    for variant in VARIANTS:
        # children first so the parents' annotations refer to defined classes
        for group in sorted(groups, key=lambda g: (ROOT_ORDER.index(g[0]), -len(g))):
            body += render_class(group, tree[group], groups, variant)
            names.append(class_name(group, variant))
        body += render_root(variant)
        names.append(
            {
                "sync": "SyncGroups",
                "async": "AsyncGroups",
                "sync_raw": "TxApiWithRawResponse",
                "async_raw": "AsyncTxApiWithRawResponse",
            }[variant]
        )
    all_names = "\n".join(f'    "{n}",' for n in sorted(names))
    return HEADER_GENERATED.format(all_names=all_names) + "\n".join(body)


def ruff_format(source: str, filename: Path) -> str:
    candidates = [Path(sys.executable).parent / "ruff", ROOT / ".venv" / "bin" / "ruff"]
    exe = next((str(c) for c in candidates if c.exists()), "ruff")
    result = subprocess.run(
        [exe, "format", "--stdin-filename", str(filename), "-"],
        input=source,
        capture_output=True,
        text=True,
        cwd=ROOT,
        check=False,
    )
    if result.returncode != 0:
        raise SystemExit(f"ruff format failed for {filename}:\n{result.stderr}")
    return result.stdout


def render_all() -> dict[Path, str]:
    spec = json.loads(SPEC.read_text(encoding="utf-8"))
    return {
        OUT_ENDPOINTS: ruff_format(render_endpoints(spec), OUT_ENDPOINTS),
        OUT_GENERATED: ruff_format(render_generated(spec), OUT_GENERATED),
    }


def main(argv: list[str]) -> int:
    outputs = render_all()
    if "--check" in argv:
        stale = [
            p.name
            for p, text in outputs.items()
            if not p.exists() or p.read_text(encoding="utf-8") != text
        ]
        if stale:
            print(f"out of date: {', '.join(stale)} — run python scripts/generate.py")
            return 1
        print("generated files are up to date")
        return 0
    for path, text in outputs.items():
        path.write_text(text, encoding="utf-8")
        print(f"wrote {path.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
