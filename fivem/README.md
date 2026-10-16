# txApi SDK for FiveM / RedM (Lua)

Official server-side Lua client for the [txApi](https://txapi.cloud) REST API — ban, kick,
whitelist, run console commands, read the event log and manage txAdmin admins from your own
FiveM or RedM resources.

- SDK version **1.0.0**, targets API version **1.8.0** (`TxApi.VERSION`, `TxApi.API_VERSION`)
- All **67** endpoints, grouped like the other official SDKs (`api.players.ban`, `api.history.revoke`, …)
- Blocking style inside threads **or** callbacks, from anywhere
- Automatic retries, rate-limit information, auto-paging iterators
- Zero dependencies — uses FXServer's built-in `PerformHttpRequest`, `promise`, `Citizen` and `json`
- Server only. MIT licensed.

> **Two things FiveM Lua cannot do, so this SDK does not either:**
> no **WebSocket** live console/server log (FXServer's Lua has no WebSocket client — use the
> REST snapshots `api.console.buffer()` and `api.serverlog.buffer()` / `api.serverlog.history()`),
> and no **webhook signature verification** (no HMAC in FXServer Lua — receive and verify webhooks
> in a small Node, Python or Java service with the official SDKs there). See [Limitations](#limitations).

---

## Contents

1. [Installation](#installation)
2. [Configuration (server.cfg)](#configuration-servercfg)
3. [Library mode](#library-mode-recommended)
4. [Exports mode](#exports-mode)
5. [Methods](#methods) — every endpoint
6. [Responses and the raw response](#responses-and-the-raw-response)
7. [Errors](#errors)
8. [Retries and timeouts](#retries-and-timeouts)
9. [Rate limits and quota](#rate-limits-and-quota)
10. [Pagination](#pagination)
11. [Example: ACE-restricted `/txban`](#example-ace-restricted-txban)
12. [Security](#security)
13. [Limitations](#limitations)
14. [How FXServer behaves (and what the SDK does about it)](#how-fxserver-behaves)
15. [Development & tests](#development--tests)

---

## Installation

1. Download `txapi-fivem-1.0.0.zip` from
   [GitHub Releases](https://github.com/txadmin-api/txapi-sdks/releases) (or
   <https://txapi.cloud/sdks/txapi-fivem-1.0.0.zip>) and extract it into your server's
   `resources/` directory. The zip contains a folder **`txapi`** — keep that name (the library
   paths below are `@txapi/...`). Checksums are in `SHA256SUMS` next to the zip.
2. Create an API key in the txApi dashboard (**API Keys**). Give it only the scopes you need.
3. Add to `server.cfg` (see the next section for the full list):

```cfg
set txapi_key "txp_xxxxxxxxxxxxxxxx"
ensure txapi
```

On start the resource prints `[txapi] ready — SDK 1.0.0, API 1.8.0, TxApi(baseUrl=…)` or the
reason it is not configured. It never prints the key.

## Configuration (server.cfg)

| Convar | Default | Meaning |
|---|---|---|
| `txapi_key` | — (required) | your API key |
| `txapi_server_id` | none | default txAdmin server for every server-scoped call; only needed when your account has several servers (`api.account.servers()` lists them). Each call can override it with `serverId`. |
| `txapi_base_url` | `https://txapi.cloud` | API host; `/api/v1` is added by the SDK |

> **Use `set`, NEVER `setr`.** `setr` *replicates* the convar to every connected client —
> every player could read your API key and ban whoever they like. `set` keeps it on the server.

```cfg
set txapi_key "txp_xxxxxxxxxxxxxxxx"     # set — NOT setr
set txapi_server_id "12"                      # optional
ensure txapi
```

## Library mode (recommended)

Load the two library files in **your** resource, server-side, in this order:

```lua
-- fxmanifest.lua of your resource
fx_version 'cerulean'
game 'gta5'
lua54 'yes'

dependency 'txapi'           -- optional, makes sure the txapi folder is present

server_scripts {
    '@txapi/lib/endpoints.lua',   -- 1. generated endpoint table
    '@txapi/lib/txapi.lua',       -- 2. the client (defines the global TxApi)
    'server.lua',                 -- 3. your code
}
```

Never list them under `client_scripts` / `shared_scripts`.

```lua
-- server.lua
local api = TxApi.new()   -- reads txapi_key / txapi_server_id / txapi_base_url

RegisterCommand('kickjohn', function(src)
    -- commands, event handlers and Citizen.CreateThread run in a FiveM thread:
    -- calls there look synchronous (they only pause this thread, never the server)
    local res, err = api.players.kick({ identifier = 'license:0123…4567', reason = 'AFK' })
    if not res then return print('kick failed: ' .. tostring(err)) end
    print(res.message)
end, true)
```

### `TxApi.new(config)`

Every option is optional; missing values come from the convars. A missing key or an invalid
option **raises** (the only place the SDK throws).

| Option | Default | Notes |
|---|---|---|
| `apiKey` | convar `txapi_key` | sent as `Authorization: Bearer <key>`; stored only in a closure, never on the client table, never printed |
| `baseUrl` | convar `txapi_base_url`, else `https://txapi.cloud` | trailing slashes stripped |
| `serverId` | convar `txapi_server_id` | default server; per-call `serverId` wins |
| `timeout` | `30` | seconds per attempt (see [timeouts](#retries-and-timeouts)) |
| `maxRetries` | `2` | `0` disables retries |
| `rateLimitRetryDelay` | `15` | seconds to wait before retrying a per-minute `429` when FXServer hides `Retry-After` |
| `userAgent` | — | appended to `txapi-fivem/1.0.0` |
| `onRequest(info)` / `onResponse(info)` | — | logging/metrics hooks; run in `pcall`, cannot break a request; the `Authorization` header they see is `Bearer ***` |
| `transport` | `PerformHttpRequest` | same signature; for tests or a proxy wrapper |

```lua
local api = TxApi.new({
    serverId = 3,
    timeout = 15,
    userAgent = GetCurrentResourceName(),
    onResponse = function(info)
        if info.error then print(('[txapi] %s %s → %s'):format(info.method, info.url, tostring(info.error))) end
    end,
})
print(api)                 --> TxApi(baseUrl=https://txapi.cloud, serverId=3, sdk=1.0.0)
```

### Blocking vs. callback

Every method works both ways:

```lua
-- 1) blocking — inside Citizen.CreateThread, an event handler or a command
Citizen.CreateThread(function()
    local res, err, resp = api.server.status()
end)

-- 2) callback — last argument is a function; works anywhere, returns immediately
api.server.status(function(res, err, resp)
    if res then print(res.serverName) end
end)
```

A blocking call outside a thread raises a clear error telling you to use a thread or a callback.
Blocking is implemented with `promise.new()` + `Citizen.Await`, so it pauses only the calling
thread.

### Arguments

* **Path parameters** come first, positionally, in URL order: `api.history.revoke('B1234', {...})`,
  `api.admins.get('John Doe')`, `api.whitelist.approvals.remove('discord:2722…')`.
  You may also put them into the table by name: `api.history.revoke({ actionId = 'B1234', reason = '…' })`.
* **Everything else** (query parameters, body fields, `serverId`) goes into **one table**:
  `api.players.ban({ identifier = 'license:…', reason = 'Cheating', durationHours = 24 })`.
* `nil` fields are simply not sent — the SDK never sends JSON `null`. Booleans are sent as given,
  so `whitelisted = false` really removes someone from the whitelist.
* Lists (`permissions`) are always sent as JSON arrays, the request body always as a JSON object —
  even when empty (`{}`), independent of how your FXServer build's `json` treats empty tables.

### Generic dispatcher

```lua
api.call('players.ban', { identifier = 'license:…', reason = 'Cheating' })           -- spec id
api.call('server.resources.restart', { name = 'chat' })                               -- or Lua name
api.call('history.revoke', { actionId = 'B1234', reason = 'appeal' }, function(res, err) end)
```

## Exports mode

No library files needed in your resource — only `ensure txapi`. The `txapi` resource keeps one
client configured from the convars (rebuilt automatically when they change). Exports are
**callback-only**: the call returns at once (`true` if started), the callback runs later.

```lua
exports.txapi:call('players.ban', {
    identifier = 'license:0123456789abcdef0123456789abcdef01234567',
    reason = 'Cheating',
    durationHours = 24,
    discordBan = true,
}, function(res, err, resp)
    if res then print('banned') else print(('%s: %s'):format(err.type, err.message)) end
end)

-- path parameters by name; method = spec id ('server.resource.restart') or Lua name
exports.txapi:call('history.revoke', { actionId = 'B1234', reason = 'appeal' }, function(res, err) end)
exports.txapi:call('server.resources.restart', { name = 'chat', serverId = 3 }, function(res, err) end)
```

| Export | Returns |
|---|---|
| `call(method, params, cb)` | `true` if started, `false` if not configured (the callback then gets a `ConfigurationError`). Raises if `cb` is missing. |
| `getRateLimit()` | `{ limit, remaining, resetSeconds }` of the last response, or `nil` |
| `isConfigured()` | `false` while `txapi_key` is missing |
| `methods()` | list of all method names |
| `version()` | `'1.0.0', '1.8.0'` (SDK, API) |

## Methods

Every endpoint has exactly one method. The **permission** column is the plan feature / key scope
the call needs. `serverId` can be added to every server-scoped call; `account.*` have none.

### account (no permission needed)
| Method | HTTP |
|---|---|
| `api.account.me()` | `GET /me` — plan, features, monthly quota, key |
| `api.account.servers()` | `GET /servers` — servers you can address with `serverId` |

### players
| Method | HTTP | Permission |
|---|---|---|
| `players.search({ query?, sortBy?, order?, filters?, offset?, cursor? })` | `GET /players/search` | `players.read` |
| `players.online({ offset?, cursor? })` | `GET /players/online` | `players.read` |
| `players.lookup({ identifier, offset?, cursor? })` | `GET /players/lookup` | `players.read` |
| `players.info({ identifier })` | `GET /players/info` | `players.read` |
| `players.stats()` | `GET /players/stats` | `players.read` |
| `players.warn({ identifier, reason })` | `POST /players/warn` | `players.write` |
| `players.ban({ identifier, reason, durationHours?, discordBan? })` | `POST /players/ban` | `players.write` |
| `players.kick({ identifier, reason? })` | `POST /players/kick` | `players.write` |
| `players.dm({ identifier, message })` | `POST /players/dm` | `players.write` |
| `players.notes({ identifier, note? })` | `POST /players/notes` | `players.write` |
| `players.whitelist({ identifier, whitelisted })` | `POST /players/whitelist` | `players.write` |

`sortBy`: `playTime` (default), `tsJoined` (alias `sessions`), `tsLastConnection` (alias
`lastConnection`). `filters`: `isOnline`, `isAdmin`, `isWhitelisted`, `hasNote` — as a
comma-separated string or a list (`{ 'isOnline', 'hasNote' }`).
`durationHours` omitted = permanent ban.

```lua
local found = api.players.search({ query = 'John', sortBy = 'lastConnection', filters = 'isOnline' })
local info  = api.players.info({ identifier = 'discord:272200000000000000' })
api.players.warn({ identifier = 'license:…', reason = 'Please stop' })
api.players.dm({ identifier = 'license:…', message = 'Check your Discord DMs' })
api.players.notes({ identifier = 'license:…', note = 'VIP since 2024' })
api.players.whitelist({ identifier = 'license:…', whitelisted = true })
```

**Discord ban (API 1.8.0):** `discordBan = true` additionally bans the player on every Discord
server whose txBot is linked to this txAdmin server. The txAdmin ban always goes first; the
Discord part never fails the call — its outcome is in `res.discord`:

```lua
local res = api.players.ban({ identifier = 'license:…', reason = 'Cheating', durationHours = 72, discordBan = true })
if res and res.discord then
    for _, g in ipairs(res.discord.guilds or {}) do
        print(g.guildName, g.ok, g.code)          -- e.g. "My RP", true, "banned"
    end
    if res.discord.skipped then print('discord skipped: ' .. res.discord.skipped) end
end
```

### history
| Method | HTTP | Permission |
|---|---|---|
| `history.bans({ order?, offset?, cursor? })` | `GET /history/bans` | `players.read` |
| `history.warns({ order?, offset?, cursor? })` | `GET /history/warns` | `players.read` |
| `history.search({ type?, admin?, searchType?, search?, order?, offset?, cursor? })` | `GET /history/search` | `players.read` |
| `history.banTemplates()` | `GET /txadmin/ban-templates` | `players.read` |
| `history.action(actionId)` | `GET /history/action/{actionId}` | `players.read` |
| `history.stats()` | `GET /history/stats` | `players.read` |
| `history.revoke(actionId, { reason?, discordUnban? })` | `POST /history/revoke/{actionId}` | `players.write` |

```lua
api.history.revoke('B1234', { reason = 'Appeal accepted', discordUnban = true })
local hits = api.history.search({ searchType = 'identifiers', search = 'license:…' })
```

### server
| Method | HTTP | Permission |
|---|---|---|
| `server.status()` | `GET /server/status` | `server.control` |
| `server.diagnostics()` | `GET /server/diagnostics` | `server.control` |
| `server.drops()` | `GET /server/drops` | `server.control` |
| `server.performance({ thread? })` | `GET /server/performance` | `server.control` |
| `server.restart()` / `server.start()` / `server.stop()` | `POST /server/{restart,start,stop}` | `server.control` |
| `server.scheduleRestart({ time })` | `POST /server/schedule-restart` | `server.control` |
| `server.skipRestart({ skip })` | `POST /server/skip-restart` | `server.control` |
| `server.kickAll({ reason? })` | `POST /server/kickall` | `server.control` |
| `server.announcement({ message })` | `POST /server/announcement` | `server.control` |
| `server.resources.start/stop/restart/ensure({ name })` | `POST /server/resource/{…}` | `server.control` |
| `server.resources.refresh()` | `POST /server/resource/refresh` | `server.control` |

`time`: `"HH:MM"` (host clock) or `"+N"` minutes. Resource names: letters, digits, `_ - .`.

```lua
api.server.announcement({ message = 'Restart in 10 minutes' })
api.server.scheduleRestart({ time = '+10' })
api.server.resources.restart({ name = 'esx_garage' })
```

### console / serverlog
| Method | HTTP | Permission |
|---|---|---|
| `console.buffer()` | `GET /console` — last console lines (snapshot) | `console.write` |
| `console.command({ command })` | `POST /console/command` (≤ 1024 chars) | `console.write` |
| `serverlog.buffer()` | `GET /server/log` | `logs.view` |
| `serverlog.history({ direction?, ref? })` | `GET /server/log/history` | `logs.view` |

### events (event log — enable it per server in the dashboard)
| Method | HTTP | Permission |
|---|---|---|
| `events.list({ type?, player?, search?, since?, until?, cursor?, limit? })` | `GET /events` | `logs.view` |
| `events.deaths({ player?, victim?, killer?, cause?, pvp?, since?, until?, cursor?, limit? })` | `GET /events/deaths` | `logs.view` |
| `events.kills({ player?, killer?, victim?, cause?, since?, until?, cursor?, limit? })` | `GET /events/kills` | `logs.view` |
| `events.leaderboard({ since?, sort?, limit? })` | `GET /events/leaderboard` | `logs.view` |
| `events.player({ player, since? })` | `GET /events/player` | `logs.view` |
| `events.summary({ since? })` | `GET /events/summary` | `logs.view` |
| `events.chat({ player?, search?, since?, until?, cursor?, limit? })` | `GET /events/chat` | `logs.view` |
| `events.commands({ … same as chat })` | `GET /events/commands` | `logs.view` |
| `events.adminActions({ admin?, kind?, search?, since?, until?, cursor?, limit? })` | `GET /events/admin-actions` | `logs.view` |
| `events.actions({ type?, player?, admin?, revoked?, since?, until?, cursor?, limit? })` | `GET /events/actions` | `logs.view` |
| `events.sessions({ player?, since?, until?, cursor?, limit? })` | `GET /events/sessions` | `logs.view` |
| `events.playtime({ since?, player?, limit? })` | `GET /events/playtime` | `logs.view` |
| `events.activity({ since?, bucket?, timeZone? })` | `GET /events/activity` | `logs.view` |
| `events.actionLog({ lines? })` | `GET /txadmin/action-log` | `logs.view` |

`since` / `until`: ISO-8601, epoch seconds/milliseconds or relative (`30m`, `24h`, `7d`).
`events.list` `type`: one type or several, as a comma-separated string or a list
(`{ 'death', 'ban' }`).

```lua
local top = api.events.leaderboard({ since = '7d', sort = 'kd', limit = 10 })
local me  = api.events.player({ player = 'license:…', since = '30d' })
```

### whitelist
| Method | HTTP | Permission |
|---|---|---|
| `whitelist.requests({ page? })` | `GET /whitelist/requests` | `players.read` |
| `whitelist.approve(requestId)` | `POST /whitelist/requests/{requestId}/approve` | `players.write` |
| `whitelist.deny(requestId)` | `POST /whitelist/requests/{requestId}/deny` | `players.write` |
| `whitelist.denyAll({ before? })` | `POST /whitelist/requests/deny-all` | `players.write` |
| `whitelist.approvals.list()` | `GET /whitelist/approvals` | `players.read` |
| `whitelist.approvals.add({ identifier })` | `POST /whitelist/approvals` | `players.write` |
| `whitelist.approvals.remove(identifier)` | `DELETE /whitelist/approvals/{identifier}` | `players.write` |

### admins (txAdmin admin accounts)
| Method | HTTP | Permission |
|---|---|---|
| `admins.list()` | `GET /txadmin/admins` | `admin.manage` |
| `admins.get(name)` | `GET /txadmin/admins/{name}` | `admin.manage` |
| `admins.permissions()` | `GET /txadmin/admins/available-permissions` | `admin.manage` |
| `admins.add({ name, permissions, citizenfxId?, discordId? })` | `POST /txadmin/admins` | `admin.manage` |
| `admins.edit(name, { permissions?, citizenfxId?, discordId? })` | `PUT /txadmin/admins/{name}` | `admin.manage` |
| `admins.delete(name)` | `DELETE /txadmin/admins/{name}` | `admin.manage` |

`admins.edit` keeps every field you do not send — so leave out what should not change.

```lua
api.admins.add({ name = 'Jane', discordId = '2722…', permissions = { 'players.ban', 'console.write' } })
api.admins.edit('Jane', { permissions = { 'players.ban' } })
```

## Responses and the raw response

Every call returns **three values**: `data, err, resp`.

* `data` — the parsed JSON (a Lua table; unknown extra fields are simply there). `nil` on error.
  A 2xx body that is not JSON is returned as a string.
* `err` — `nil` on success, otherwise an error table (see below).
* `resp` — `{ status, headers, rateLimit, body, text, attempts, durationMs }`, also on errors that
  got an HTTP answer. `api.lastResponse` holds the latest one.

Mutations answer `{ success, message, discord? }`. JSON `null` values may arrive as `json.null`
(current FXServer) or `nil` (older builds) — test them with `TxApi.isNull(v)`.

## Errors

Errors are **returned, not thrown** — check the second value. Each error is a table:

| Field | Meaning |
|---|---|
| `type` | one of the names below |
| `message` | the API's `error` text, else the body, else the reason phrase |
| `status` | HTTP status (absent for transport/validation errors) |
| `body` | parsed error body (table) or the raw text |
| `headers`, `rateLimit` | response headers (empty behind FXServer, see below) and the last known rate limit |
| `retryAfter`, `quotaExhausted` | for `RateLimitError` |
| `field` | for `ValidationError` |
| `endpoint`, `method`, `path`, `attempts` | what failed (the key is never part of an error) |

`tostring(err)` gives e.g. `TxAdminRejectedError (HTTP 422): Player is offline`.
`TxApi.isError(v)` tells SDK errors apart.

| `type` | Status | Meaning |
|---|---|---|
| `BadRequestError` | 400, 405, 415 | invalid input |
| `AuthenticationError` | 401 | key missing, invalid, revoked or expired |
| `PermissionDeniedError` | 403 | plan feature / key scope missing, IP not allowed, server disabled, txAdmin account lacks a permission |
| `NotFoundError` | 404 | unknown player/action/admin/server |
| `ConflictError` | 409 | e.g. no server configured |
| `TxAdminRejectedError` | 422 | txAdmin refused the action (e.g. player offline) |
| `RateLimitError` | 429 | see [rate limits](#rate-limits-and-quota) |
| `TxAdminUnavailableError` | 502 | txAdmin unreachable or answered garbage |
| `ServerError` | other 5xx | |
| `ApiError` | any other non-2xx | |
| `ConnectionError` | — | txApi could not be reached (DNS, TLS, refused) |
| `TimeoutError` | — | no answer in time |
| `ValidationError` | — | client-side check failed, **no request was sent** |
| `ConfigurationError` | — | exports mode only: `txapi_key` not set |

```lua
local res, err = api.players.kick({ identifier = id, reason = 'AFK' })
if not res then
    if err.type == 'TxAdminRejectedError' then print('player is not online')
    elseif err.type == 'PermissionDeniedError' then print('key or plan lacks players.write: ' .. err.message)
    elseif err.type == 'RateLimitError' and err.quotaExhausted then print('monthly quota used up')
    else print(tostring(err)) end
end
```

**Client-side validation** (before any network call): required fields present and not blank,
`maxLength` (reason/message 512, note 2000, resource name 128, command 1024 — counted in
characters), basic types (`durationHours` must be a number, `permissions` a list of strings),
and **unknown parameter names are rejected** — in Lua there are no types to catch a typo, and
`durationhours = 24` silently dropped would turn a 24-hour ban into a permanent one. Everything
else is checked by the server.

## Retries and timeouts

* **429 with a known wait** (`Retry-After` ≤ 60 s): retried for **any** method after that delay —
  the request was not executed.
* **429 behind FXServer** (headers hidden, see below) and not the monthly quota: retried after
  `rateLimitRetryDelay` (15 s).
* **429 monthly quota**: never retried.
* **Connection errors, timeouts, 502, 503, 504**: retried for **GET only** — a retried POST could
  ban twice or restart a server twice.
* Backoff `min(8 s, 0.5 s · 2^attempt)` with full jitter, unless `Retry-After` says otherwise.
* `maxRetries = 0` disables everything. Waiting uses `Citizen.Wait`, so only the calling thread
  pauses. To stop paging early, simply `break` out of the loop.

**Timeouts:** `PerformHttpRequest` has no timeout option, so `timeout` (default 30 s) is enforced
by the SDK with a timer: after it, the call returns a `TimeoutError` and a late answer is ignored
(the underlying request cannot be cancelled). Independently, **FXServer itself aborts a request
when no response byte has arrived after ~5 seconds** (`CURL error code 42`, reported as
`TimeoutError`). Almost every call answers well within that; a slow txAdmin behind a POST may hit
it — the action may still have happened, which is why POSTs are not retried. Check with a GET
(e.g. `history.search`) before trying again.

## Rate limits and quota

Each plan has a per-minute limit and a monthly quota. Successful responses carry
`X-RateLimit-Limit/-Remaining/-Reset`; the SDK parses them (any header case) into
`{ limit, remaining, resetSeconds }`:

```lua
local res, err, resp = api.players.stats()
print(resp.rateLimit and resp.rateLimit.remaining)   -- this response
print(api.lastRateLimit and api.lastRateLimit.remaining)   -- latest seen by this client
local me = api.account.me()                          -- monthly quota: me.quota.used / limit / resetsAt
```

`RateLimitError.quotaExhausted` is `true` for the monthly quota (resets on the 1st, UTC — retrying
does not help) and `false` for the per-minute limit. Behind FXServer, error responses arrive
**without headers**, so the SDK recognises the quota by the API's message
(`Monthly request limit reached` / code `QUOTA`) and `err.retryAfter` is `nil`; `err.rateLimit` is
then the last limit seen on a successful response.

## Pagination

List endpoints return at most 100 rows plus a cursor. The SDK pages for you — **blocking style,
inside a thread**. For each paged method there are two helpers:

* `<method>Pages(opts)` — yields whole pages (`page.players` / `page.history` / `page.events`)
* `<method>All(opts)` — yields single rows

| Helpers | Next page uses |
|---|---|
| `players.searchPages/searchAll`, `players.onlinePages/onlineAll`, `players.lookupPages/lookupAll` | `offset` = last row's sort key (`playTime`, `tsJoined`, `tsLastConnection`; aliases `sessions`, `lastConnection`), `cursor` = its `license` |
| `history.bansPages/bansAll`, `history.warnsPages/warnsAll`, `history.searchPages/searchAll` | `offset` = last row's `timestamp`, `cursor` = its `id` |
| `events.list…`, `deaths…`, `kills…`, `chat…`, `commands…`, `adminActions…`, `actions…`, `sessions…` (`Pages`/`All`) | `cursor` = `nextCursor` |

Paging stops on `hasReachedEnd`, a missing/null `nextCursor`, an empty page, or a cursor that
does not move. Errors do not throw: the loop ends and the error is in `.err`.

```lua
Citizen.CreateThread(function()
    local bans = api.history.bansAll({ order = 'desc' })
    for ban in bans do
        print(ban.id, ban.playerName, ban.reason)
    end
    if bans.err then print('stopped: ' .. tostring(bans.err)) end

    for page in api.events.deathsPages({ pvp = true, since = '24h', limit = 200 }) do
        print(#page.events .. ' PvP deaths on this page')
    end
end)
```

Each page is a normal request (counts against the quota, gets retries).

## Example: ACE-restricted `/txban`

A complete example resource lives in [`examples/txapi-example/`](examples/txapi-example) (not
loaded by default). It adds `/txban <name> <hours|0> <reason>` for the console and players with
the ACE `txapi.ban`, a console-only `/txstatus`, and a `banOnline(source, reason)` export:

```cfg
add_ace group.admin txapi.ban allow
ensure txapi
ensure txapi-example
```

```lua
RegisterCommand('txban', function(src, args)
    if src ~= 0 and not IsPlayerAceAllowed(src, 'txapi.ban') then return end
    local found = api.players.search({ query = args[1] })
    local target = found and found.players[1]
    if not target then return end
    local hours = tonumber(args[2]) or 0
    local res, err = api.players.ban({
        identifier = 'license:' .. target.license,
        reason = table.concat(args, ' ', 3),
        durationHours = hours > 0 and hours or nil,   -- nil = permanent
    })
    print(res and 'banned' or tostring(err))
end, false)
```

More: [`examples/library_usage.lua`](examples/library_usage.lua),
[`examples/exports_usage.lua`](examples/exports_usage.lua),
[`examples/pagination.lua`](examples/pagination.lua).

## Security

* **The key is a server secret.** `set txapi_key`, never `setr`. Never put it in a client script,
  `shared_scripts`, a NUI page or a file listed under `files`.
* Never load `@txapi/lib/*.lua` as client or shared script — the resource has no client part on
  purpose.
* Don't build endpoints that let players trigger API calls with their own input without checking
  permissions (ACE) first: anything the key can do, a player could make your server do.
* Give the key only the scopes it needs (dashboard → API keys), and an IP allowlist with your
  server's public IP.
* The SDK never logs the key; `tostring(api)` and errors don't contain it, hooks see `Bearer ***`.
  Redirects are not followed (`followLocation = false`), so the `Authorization` header cannot be
  forwarded to another host.

## Limitations

* **No WebSockets.** FXServer's Lua has no WebSocket client, so `/ws/console` and
  `/ws/serverlog` are not available. Use `api.console.buffer()` / `api.serverlog.buffer()` for a
  snapshot, `api.serverlog.history()` for older log pages, and the event log (`api.events.*`) for
  structured history. For a live stream, run a tiny Node/Python/Java process with the official
  SDK of that language.
* **No webhook verification.** There is no HMAC-SHA256 in FXServer Lua, and FXServer is not a
  good public HTTP endpoint anyway. Receive txApi webhooks in a small service using the
  TypeScript, Python or Java SDK (`verifyWebhook`, checks `X-Txapi-Signature` on the raw body) and
  forward what you need. The event names are available as `TxApi.WEBHOOK_EVENTS`.
* **Timeouts cannot cancel** the underlying request (see above), and FXServer's ~5 s
  first-byte limit applies.
* **Headers of error responses are not visible** in FXServer (see next section).
* `maxLength` is counted in characters; the server counts UTF-16 units, so text with many emoji
  can still be rejected by the server (`BadRequestError`).

## How FXServer behaves

The SDK was written against FXServer's actual implementation
(`citizenfx/fivem`: `data/shared/citizen/scripting/lua/scheduler.lua`,
`code/components/citizen-server-impl/src/HttpScriptFunctions.cpp`,
`code/components/http-client/src/HttpClient.cpp`) and the tests' mock reproduces it:

| Situation | `PerformHttpRequest` callback | SDK |
|---|---|---|
| 2xx/3xx | `(status, body, headers, nil)` — header names as sent (any case), repeated headers as an array | case-insensitive header lookup, first value of arrays |
| ≥ 400 | `(status, nil, {}, "HTTP <status>: <body>")` — **no headers** | body taken from the 4th argument; quota recognised by its message |
| transport error | `(0, nil, {}, "<text> - CURL error code N (…)")` | `ConnectionError`, or `TimeoutError` for codes 28/42 |
| no byte for ~5 s | aborted by FXServer (`CURL error code 42`) | `TimeoutError` |
| options | only `followLocation` (default `true`) | sends `followLocation = false` |

The global `json` is lua-rapidjson in current builds (dkjson in old ones). They disagree on empty
tables (`{}` vs `[]`) and on `null` (`json.null` vs `nil`); the SDK marks every table it encodes
with `__jsontype` (honoured by both) and treats both `null` forms as absent.

## Development & tests

```sh
# from the fivem/ directory
sh tests/run.sh                     # generator freshness check (Node) + luac -p + all tests
lua5.4 tests/run.lua                # tests only (plain Lua 5.4, offline)
node scripts/gen-endpoints.mjs      # regenerate lib/endpoints.lua after an API change
```

The tests mock the FiveM runtime (`tests/harness/fivem.lua`: scheduler, `promise`, `Citizen.*`,
`PerformHttpRequest` with FXServer's error semantics, convars, exports) and its `json`
(`tests/harness/json.lua`, both empty-table/null behaviours). The coverage test calls all 67
methods and compares the recorded `METHOD path` set with `../spec/endpoints.json`, so a new API
endpoint fails the suite until it is added here.

## License

MIT — see [LICENSE](LICENSE).
