# txapi-sdk — official Python SDK for the txApi

Typed Python client for the [txApi](https://txapi.cloud) REST and WebSocket API: manage your
FiveM servers' txAdmin — players, bans, warnings, whitelist, server control, the live console,
the stored event log and txAdmin admins — from Python.

- Sync (`TxApi`) **and** async (`AsyncTxApi`) clients with the identical surface
- Every one of the 67 endpoints of API **1.8.0**, fully type-hinted (`mypy --strict` clean),
  responses as `TypedDict`s
- Automatic retries that never repeat a ban or a restart, rate-limit information on every call
- Auto-paging iterators, webhook signature verification, live console / server log streams
- Python 3.9+, one runtime dependency (`httpx`); `websockets` only for live streams

```bash
pip install txapi-sdk            # REST client + webhooks
pip install "txapi-sdk[live]"    # + live console / server-log streams (websockets)
```

The distribution is called **`txapi-sdk`** (the name `txapi` on PyPI belongs to someone else);
the import stays `import txapi`. Every release is also attached to
[GitHub Releases](https://github.com/txadmin-api/txapi-sdks/releases) and served from
`https://txapi.cloud/sdks/` (checksums in `SHA256SUMS`), for setups without PyPI access:

```bash
pip install "txapi-sdk[live] @ https://txapi.cloud/sdks/txapi_sdk-1.0.0-py3-none-any.whl"
```

## Contents

- [Quickstart](#quickstart)
- [Configuration](#configuration)
- [Calling endpoints](#calling-endpoints)
- [Errors](#errors)
- [Retries](#retries)
- [Rate limits and raw responses](#rate-limits-and-raw-responses)
- [Pagination](#pagination)
- [Webhooks](#webhooks)
- [Live console and server log](#live-console-and-server-log)
- [Hooks, custom HTTP clients, timeouts](#hooks-custom-http-clients-timeouts)
- [Endpoint reference](#endpoint-reference)
- [Development](#development)

## Quickstart

Create an API key in the dashboard (**API keys**), then:

```python
from txapi import TxApi

with TxApi(api_key="txp_...", server_id=1) as client:
    page = client.players.search(query="john")
    player = page["players"][0]
    client.players.ban(
        identifier=f"license:{player['license']}",
        reason="Cheating",
        duration_hours=168,  # omit (or 0) for a permanent ban
    )
```

The same with `asyncio`:

```python
import asyncio
from txapi import AsyncTxApi

async def main() -> None:
    async with AsyncTxApi(server_id=1) as client:  # key from TXAPI_KEY
        status = await client.server.status()
        print(status["serverName"], "online" if status.get("online") else "offline")
        await client.server.announcement(message="Restart in 5 minutes")

asyncio.run(main())
```

> **Never log or commit your API key.** It is sent as `Authorization: Bearer <key>`; the
> clients' and errors' `repr()`/`str()` never contain it. Prefer the `TXAPI_KEY` environment
> variable over hard-coding it. The request headers passed to `on_request` hooks *do* contain
> it — don't log them.

## Configuration

| Argument | Default | |
|---|---|---|
| `api_key` | env `TXAPI_KEY` | required — a missing key raises `ValidationError` at construction |
| `base_url` | env `TXAPI_BASE_URL`, else `https://txapi.cloud` | trailing slash stripped, `/api/v1` is added by the SDK |
| `server_id` | — | default server for every server-scoped call; each call can pass `server_id=` |
| `timeout` | `30` | seconds per attempt; each call can pass `timeout=` |
| `max_retries` | `2` | retries after the first attempt, `0` disables retries |
| `user_agent` | — | appended to `txapi-python/1.0.0` |
| `http_client` | — | your own `httpx.Client` / `httpx.AsyncClient` (not closed by the SDK) |
| `on_request` / `on_response` | — | hooks for logging/metrics |

```python
client = TxApi(server_id=1, timeout=10, max_retries=3, user_agent="my-panel/2.1")
print(client.account.servers())                # account-level: no serverId is sent
client.server.status(server_id=2)              # override the default server for one call
```

`txapi.__version__` is the SDK version (`1.0.0`), `txapi.API_VERSION` the API version it was
built for (`1.8.0`).

## Calling endpoints

Endpoints are grouped like the API: `client.account`, `players`, `history`, `server`
(+ `server.resources`), `console`, `serverlog`, `events`, `whitelist` (+ `whitelist.approvals`)
and `admins`. Path parameters are positional, everything else is keyword-only and
snake_case (sent as camelCase). `None` arguments are omitted — never sent as `null`.
Every method's docstring names the permission it needs.

```python
me = client.account.me()
print(me["plan"]["code"], me["quota"].get("remaining"), me["plan"]["features"])

# Players
client.players.info(identifier="discord:272800190639898628")
client.players.warn(identifier="license:abc…", reason="Mic spam")
client.players.kick(identifier="license:abc…", reason="AFK")
client.players.dm(identifier="license:abc…", message="Please read the rules")
client.players.notes(identifier="license:abc…", note="Watch this one")
client.players.whitelist(identifier="license:abc…", whitelisted=True)

# Bans incl. Discord (API 1.8.0): also ban on every Discord server linked via txBot
result = client.players.ban(identifier="license:abc…", reason="Cheating", discord_ban=True)
for guild in result.get("discord", {}).get("guilds", []):
    print(guild["guildName"], guild["code"])     # banned, missing_permission, hierarchy, …
client.history.revoke("BKL5-3SH4", reason="Appeal accepted", discord_unban=True)

# Server control
client.server.restart()
client.server.schedule_restart(time="+30")       # in 30 minutes; or "04:00" (host clock)
client.server.skip_restart(skip=True)
client.server.kick_all(reason="Maintenance")
client.server.resources.restart(name="es_extended")

# Console (REST) — see below for the live stream
client.console.command(command="say Hello")
print(client.console.buffer()["lines"][-20:])

# Stored event log
board = client.events.leaderboard(since="7d", sort="kills", limit=10)
stats = client.events.player(player="license:abc…", since="30d")
deaths = client.events.deaths(pvp=True, since="24h")
client.events.list(type=["ban", "warn"], since="2026-10-01T00:00:00Z")

# Whitelist
client.whitelist.approve("R1234")
client.whitelist.approvals.add(identifier="discord:272800190639898628")
client.whitelist.approvals.remove("discord:272800190639898628")   # percent-encoded for you

# txAdmin admins
client.admins.add(name="Moderator Bob", permissions=["players.ban", "console.write"])
client.admins.edit("Moderator Bob", discord_id="123")   # only the fields you pass change
client.admins.delete("Moderator Bob")
```

Responses are parsed JSON typed as `TypedDict`s (`txapi.types`). They are plain dicts at
runtime, so fields the API adds in later minor versions never break anything. txAdmin
passthrough endpoints (e.g. `players.info`, `server.diagnostics`) return `JsonValue` (`Any`).
Write endpoints return `{"success": True, "message": "…", "discord"?: …}`.

Required arguments are checked before anything is sent: blank required fields and values
longer than the API allows (ban reason 512, note 2000, resource name 128, console command 1024
characters) raise `txapi.ValidationError` without a request.

## Errors

Every exception derives from `txapi.TxApiError` with `status`, `message` (the API's `error`
text, else the body, else the reason phrase), parsed `body`, `headers` and `rate_limit`.

| Status | Exception | Typical cause |
|---|---|---|
| 400, 405, 415 | `BadRequestError` | invalid input |
| 401 | `AuthenticationError` | key missing, invalid, revoked or expired |
| 403 | `PermissionDeniedError` | plan feature / key scope missing, IP not allowed, server disabled |
| 404 | `NotFoundError` | unknown player, action, admin or server |
| 409 | `ConflictError` | e.g. no txAdmin server configured |
| 422 | `TxAdminRejectedError` | txAdmin refused the action (e.g. player offline) |
| 429 | `RateLimitError` | `retry_after` seconds, or `quota_exhausted` (no `Retry-After`) |
| 502 | `TxAdminUnavailableError` (a `ServerError`) | txAdmin unreachable |
| other 5xx | `ServerError` | |
| — | `APIConnectionError`, `APITimeoutError` | network problems, timeouts |
| — | `ValidationError` (also a `ValueError`) | client-side check, nothing was sent |
| — | `WebhookVerificationError` | see [Webhooks](#webhooks) |
| — | `CommandError` | live console answered a command with an error |

```python
from txapi import NotFoundError, RateLimitError, TxAdminRejectedError, TxApiError

try:
    client.players.kick(identifier="license:abc…", reason="AFK")
except TxAdminRejectedError as exc:
    print("txAdmin refused:", exc.message)        # e.g. the player is offline
except NotFoundError:
    print("No such player")
except RateLimitError as exc:
    if exc.quota_exhausted:
        print("Monthly quota used up — resets on the 1st, 00:00 UTC")
    else:
        print(f"Too fast, retry in {exc.retry_after} s")
except TxApiError as exc:
    print(exc.status, exc.message, exc.body)
```

`APIConnectionError` and `APITimeoutError` are named so to not shadow Python's built-in
`ConnectionError`/`TimeoutError` (the spec's names).

## Retries

- **429 with `Retry-After` ≤ 60 s** is retried for every method after exactly that delay —
  the request was not executed. 429 *without* `Retry-After` means the monthly quota is used
  up and is never retried.
- **Connection errors, timeouts, 502, 503, 504** are retried for **GET only**. A retried
  POST could ban twice or restart a server twice.
- Backoff: full jitter, `random() * min(8 s, 0.5 s · 2^attempt)`, unless `Retry-After` says otherwise.
- `max_retries=0` disables everything. Cancelling an `AsyncTxApi` call (task cancel) stops
  waiting immediately.

## Rate limits and raw responses

Every response carries the per-minute limit; the SDK parses it into
`RateLimit(limit, remaining, reset_seconds)`:

```python
client.server.status()
rl = client.last_rate_limit          # None before the first response
print(rl.remaining, "of", rl.limit, "left, resets in", rl.reset_seconds, "s")
```

Errors carry it too (`exc.rate_limit`). For status code and headers together with the
parsed data, use `with_raw_response` — the same methods, returning `txapi.ApiResponse`:

```python
raw = client.with_raw_response.players.ban(identifier="license:abc…", reason="x")
raw.data            # {"success": True, "message": "…"}
raw.status_code     # 200
raw.headers         # httpx.Headers
raw.rate_limit      # RateLimit(...)
raw.http_response   # the httpx.Response
```

## Pagination

List endpoints return one page; the `iter_*` variants page through everything lazily
(the next page is fetched only when the current one is used up):

```python
for player in client.players.iter_search(sort_by="tsLastConnection"):
    print(player["displayName"])

for ban in client.history.iter_bans():
    if ban.get("isRevoked"):
        continue
    print(ban["id"], ban["playerName"], ban["reason"])

for death in client.events.iter_deaths(since="7d", pvp=True):
    print(death["killer"], "→", death["victim"])
```

```python
async for row in client.events.iter_admin_actions(since="24h"):   # AsyncTxApi
    print(row)
```

| Iterators | Cursor |
|---|---|
| `players.iter_search` / `iter_online` / `iter_lookup` | `offset` = last row's sort key (`playTime`, `tsJoined`, `tsLastConnection`; aliases `sessions`, `lastConnection`), `cursor` = its `license` |
| `history.iter_bans` / `iter_warns` / `iter_search` | `offset` = last row's `timestamp`, `cursor` = its `id` |
| `events.iter_list` / `iter_deaths` / `iter_kills` / `iter_chat` / `iter_commands` / `iter_admin_actions` / `iter_actions` / `iter_sessions` | `cursor` = `nextCursor` |

Iteration stops on `hasReachedEnd`, a missing `nextCursor` or an empty page. To page by
hand, pass `offset=`/`cursor=` to the non-iterating method.

## Webhooks

Webhooks (dashboard → **Webhooks**) are signed with your endpoint secret (`whsec_…`):
`X-Txapi-Signature: t=<unix seconds>,v1=<hex HMAC-SHA256>`. `txapi.verify_webhook` checks the
signature (constant time, several `v1` entries allowed during secret rotation) and the
timestamp (default tolerance 300 s), and returns the event
`{id, event, createdAt, serverId, data}`:

```python
from txapi import WebhookVerificationError, verify_webhook

event = verify_webhook(raw_body, signature_header, secret)
```

> **Verify the raw request body — the exact bytes you received.** Parsing the JSON and
> serialising it again (`json.dumps(request.json())`) changes whitespace and key order and
> the signature will never match.

On failure it raises `WebhookVerificationError` with a `reason`: `missing_timestamp`,
`missing_signature`, `malformed_header`, `timestamp_out_of_tolerance`, `signature_mismatch`,
`invalid_body` or `missing_secret`. All event names are in `txapi.WEBHOOK_EVENTS`
(and the `txapi.WebhookEventName` literal type); header names in `txapi.webhooks`.

**Flask**

```python
from flask import Flask, abort, request
from txapi import WebhookVerificationError, verify_webhook

app = Flask(__name__)

@app.post("/txapi/webhook")
def txapi_webhook():
    try:
        event = verify_webhook(
            request.get_data(),                          # raw bytes
            request.headers.get("X-Txapi-Signature"),
            WEBHOOK_SECRET,
        )
    except WebhookVerificationError as exc:
        abort(400, exc.reason)
    if event["event"] == "player.banned":
        notify_staff(event["data"])
    return {"ok": True}
```

**FastAPI** — see [`examples/fastapi_webhook.py`](examples/fastapi_webhook.py):

```python
@app.post("/txapi/webhook")
async def txapi_webhook(request: Request):
    raw = await request.body()                               # raw bytes
    try:
        event = verify_webhook(raw, request.headers.get("X-Txapi-Signature"), WEBHOOK_SECRET)
    except WebhookVerificationError as exc:
        raise HTTPException(400, exc.reason)
    ...
    return {"ok": True}
```

Answer with a 2xx quickly; failed deliveries are retried by txApi.

## Live console and server log

Needs `pip install "txapi-sdk[live]"`. `client.console.connect()` (permission `console.write`)
and `client.serverlog.connect()` (`logs.view`) return asyncio streams:

```python
import asyncio
from txapi import AsyncTxApi
from txapi.live import CloseEvent, InitEvent, LogEvent

async def main() -> None:
    async with AsyncTxApi(server_id=1) as client:
        async with client.console.connect() as console:
            print(await console.send_command("status"))   # resolves with txAdmin's ack
            async for event in console:
                if isinstance(event, InitEvent):           # buffer after every (re)connect
                    print(len(event.items), "buffered lines")
                elif isinstance(event, LogEvent):          # one event per console line
                    print(event.item)
                elif isinstance(event, CloseEvent) and not event.will_reconnect:
                    print("closed:", event.code, event.reason)

asyncio.run(main())
```

Events (`txapi.live`): `OpenEvent(reconnected)`, `InitEvent(items)`, `LogEvent(item)`,
`ErrorEvent(message)`, `CloseEvent(code, reason, will_reconnect, by_user)`,
`ReconnectingEvent(attempt, delay, code, reason)` and, with `raw_frames=True`,
`FrameEvent(frame)`. Instead of iterating you can pass `on_event=` (sync or async callback).

- **`send_command(cmd)`** validates the length (≤ 1024), sends `{"type": "command"}` and
  returns the next `ack` — or raises `CommandError` for the next `error` frame. Commands are
  correlated first-in-first-out; timeout 10 s (`command_timeout=` / `timeout=`) raises
  `APITimeoutError`. The command's *output* arrives later as `LogEvent`s. An `error` frame
  that answers no pending command is emitted as `ErrorEvent`.
- **Reconnects** automatically after close codes 1001, 1006, 1011, 1012 and network loss
  (backoff 1 s → 30 s with jitter; `reconnect=`, `max_reconnect_attempts=`,
  `initial_reconnect_delay=`, `max_reconnect_delay=`). After a reconnect the server sends the
  full buffer again as `InitEvent` — de-duplicate or ignore it.
- **No reconnect** after 1008 (authentication, permission, quota, too slow — `event.reason`
  and `stream.close_reason` tell you what to fix), 1009, a refused handshake, or `close()`.
  Iteration then ends.
- Server-log `LogEvent.item`s are txAdmin's structured entries (`dict`) or plain strings.
- The key travels in the WebSocket URL's query string (that is how the server authenticates
  streams); it is never included in events or error messages.

The synchronous `TxApi` hands out the same asyncio streams — use them inside
`asyncio.run(...)`; there is no blocking stream API. See
[`examples/live_console.py`](examples/live_console.py).

## Hooks, custom HTTP clients, timeouts

```python
import httpx, logging

def log_request(request: httpx.Request) -> None:
    logging.info("→ %s %s", request.method, request.url.path)   # never log request.headers

def log_response(response: httpx.Response) -> None:
    logging.info("← %s", response.status_code)

client = TxApi(
    http_client=httpx.Client(limits=httpx.Limits(max_connections=20)),
    on_request=log_request,       # called before every attempt
    on_response=log_response,     # called for every response, also errors and retries
    timeout=15,
)
client.server.diagnostics(timeout=60)   # per call
```

Hooks can never break a request — exceptions inside them are logged and ignored. With
`AsyncTxApi` they may be coroutines. A client you pass in is not closed by `close()`.

## Endpoint reference

| Method | HTTP | Permission |
|---|---|---|
| `client.account.me()` | `GET /me` | any key |
| `client.account.servers()` | `GET /servers` | any key |
| `client.players.search()` · `iter_search()` | `GET /players/search` | `players.read` |
| `client.players.online()` · `iter_online()` | `GET /players/online` | `players.read` |
| `client.players.lookup()` · `iter_lookup()` | `GET /players/lookup` | `players.read` |
| `client.players.info()` | `GET /players/info` | `players.read` |
| `client.players.stats()` | `GET /players/stats` | `players.read` |
| `client.players.warn()` | `POST /players/warn` | `players.write` |
| `client.players.ban()` | `POST /players/ban` | `players.write` |
| `client.players.kick()` | `POST /players/kick` | `players.write` |
| `client.players.dm()` | `POST /players/dm` | `players.write` |
| `client.players.notes()` | `POST /players/notes` | `players.write` |
| `client.players.whitelist()` | `POST /players/whitelist` | `players.write` |
| `client.history.bans()` · `iter_bans()` | `GET /history/bans` | `players.read` |
| `client.history.warns()` · `iter_warns()` | `GET /history/warns` | `players.read` |
| `client.history.search()` · `iter_search()` | `GET /history/search` | `players.read` |
| `client.history.ban_templates()` | `GET /txadmin/ban-templates` | `players.read` |
| `client.history.action()` | `GET /history/action/{actionId}` | `players.read` |
| `client.history.stats()` | `GET /history/stats` | `players.read` |
| `client.history.revoke()` | `POST /history/revoke/{actionId}` | `players.write` |
| `client.server.status()` | `GET /server/status` | `server.control` |
| `client.server.diagnostics()` | `GET /server/diagnostics` | `server.control` |
| `client.server.drops()` | `GET /server/drops` | `server.control` |
| `client.server.performance()` | `GET /server/performance` | `server.control` |
| `client.server.restart()` | `POST /server/restart` | `server.control` |
| `client.server.start()` | `POST /server/start` | `server.control` |
| `client.server.stop()` | `POST /server/stop` | `server.control` |
| `client.server.schedule_restart()` | `POST /server/schedule-restart` | `server.control` |
| `client.server.skip_restart()` | `POST /server/skip-restart` | `server.control` |
| `client.server.kick_all()` | `POST /server/kickall` | `server.control` |
| `client.server.announcement()` | `POST /server/announcement` | `server.control` |
| `client.server.resources.start()` | `POST /server/resource/start` | `server.control` |
| `client.server.resources.stop()` | `POST /server/resource/stop` | `server.control` |
| `client.server.resources.restart()` | `POST /server/resource/restart` | `server.control` |
| `client.server.resources.ensure()` | `POST /server/resource/ensure` | `server.control` |
| `client.server.resources.refresh()` | `POST /server/resource/refresh` | `server.control` |
| `client.console.buffer()` | `GET /console` | `console.write` |
| `client.console.command()` | `POST /console/command` | `console.write` |
| `client.serverlog.buffer()` | `GET /server/log` | `logs.view` |
| `client.serverlog.history()` | `GET /server/log/history` | `logs.view` |
| `client.events.list()` · `iter_list()` | `GET /events` | `logs.view` |
| `client.events.deaths()` · `iter_deaths()` | `GET /events/deaths` | `logs.view` |
| `client.events.kills()` · `iter_kills()` | `GET /events/kills` | `logs.view` |
| `client.events.leaderboard()` | `GET /events/leaderboard` | `logs.view` |
| `client.events.player()` | `GET /events/player` | `logs.view` |
| `client.events.summary()` | `GET /events/summary` | `logs.view` |
| `client.events.chat()` · `iter_chat()` | `GET /events/chat` | `logs.view` |
| `client.events.commands()` · `iter_commands()` | `GET /events/commands` | `logs.view` |
| `client.events.admin_actions()` · `iter_admin_actions()` | `GET /events/admin-actions` | `logs.view` |
| `client.events.actions()` · `iter_actions()` | `GET /events/actions` | `logs.view` |
| `client.events.sessions()` · `iter_sessions()` | `GET /events/sessions` | `logs.view` |
| `client.events.playtime()` | `GET /events/playtime` | `logs.view` |
| `client.events.activity()` | `GET /events/activity` | `logs.view` |
| `client.events.action_log()` | `GET /txadmin/action-log` | `logs.view` |
| `client.whitelist.requests()` | `GET /whitelist/requests` | `players.read` |
| `client.whitelist.approve()` | `POST /whitelist/requests/{requestId}/approve` | `players.write` |
| `client.whitelist.deny()` | `POST /whitelist/requests/{requestId}/deny` | `players.write` |
| `client.whitelist.deny_all()` | `POST /whitelist/requests/deny-all` | `players.write` |
| `client.whitelist.approvals.list()` | `GET /whitelist/approvals` | `players.read` |
| `client.whitelist.approvals.add()` | `POST /whitelist/approvals` | `players.write` |
| `client.whitelist.approvals.remove()` | `DELETE /whitelist/approvals/{identifier}` | `players.write` |
| `client.admins.list()` | `GET /txadmin/admins` | `admin.manage` |
| `client.admins.get()` | `GET /txadmin/admins/{name}` | `admin.manage` |
| `client.admins.permissions()` | `GET /txadmin/admins/available-permissions` | `admin.manage` |
| `client.admins.add()` | `POST /txadmin/admins` | `admin.manage` |
| `client.admins.edit()` | `PUT /txadmin/admins/{name}` | `admin.manage` |
| `client.admins.delete()` | `DELETE /txadmin/admins/{name}` | `admin.manage` |

Plus `client.console.connect()` (`WS /ws/console`, `console.write`) and
`client.serverlog.connect()` (`WS /ws/serverlog`, `logs.view`).

## Development

```bash
python -m venv .venv && .venv/bin/pip install -e ".[live,dev]"
.venv/bin/pytest                    # offline: mock HTTP transport + local WebSocket server
.venv/bin/mypy --strict src
.venv/bin/ruff check src scripts tests examples && .venv/bin/ruff format --check src scripts tests examples
python scripts/generate.py          # regenerate src/txapi/_endpoints.py + _generated.py
python scripts/generate.py --check  # CI: fails if the generated code is stale
```

The endpoint methods are generated from `../spec/endpoints.json` (itself generated from the
API catalog). When the API changes, regenerate; the coverage test fails until every endpoint
has a method.

## License

MIT — see [LICENSE](LICENSE).
