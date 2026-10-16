# Changelog

All notable changes to the txApi FiveM SDK are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the
project uses [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-10-14

First release, built against txApi **1.8.0** (`spec/endpoints.json`).

### Added

- Server-only FiveM resource `txapi` (fx_version `cerulean`, games `gta5` and `rdr3`,
  Lua 5.4, no client scripts).
- All **67** REST endpoints, grouped by area (`api.players.ban(...)`,
  `api.server.status()`, `api.events.leaderboard(...)`, ...). `lib/endpoints.lua` is
  generated from `spec/endpoints.json` by `node scripts/gen-endpoints.mjs`.
- **Library mode**: load `@txapi/lib/endpoints.lua` + `@txapi/lib/txapi.lua` into your
  own resource and call `TxApi.new({...})`. Calls block the current thread
  (`promise` + `Citizen.Await`) and return `data, err, resp`; pass a function as the
  last argument to get a callback instead.
- **Exports mode**: `exports.txapi:call('players.ban', params, cb)` plus `getRateLimit`,
  `isConfigured`, `methods` and `version`, configured from convars.
- Configuration via `TxApi.new` or the convars `txapi_key`, `txapi_server_id` and
  `txapi_base_url`. The key is never printed, never logged and never sent anywhere but
  the `Authorization: Bearer` header.
- Request building per the SDK spec: `serverId` on every server-scoped call, path
  parameters percent-encoded, empty arrays and objects encoded as JSON arrays/objects
  regardless of the runtime's JSON library, client-side validation (required fields,
  types, `maxLength`, unknown parameter names).
- Error values instead of exceptions (`BadRequestError`, `AuthenticationError`,
  `PermissionDeniedError`, `NotFoundError`, `ConflictError`, `TxAdminRejectedError`,
  `RateLimitError`, `TxAdminUnavailableError`, `ServerError`, `ApiError`,
  `ConnectionError`, `TimeoutError`, `ValidationError`, plus `ConfigurationError` in
  exports mode) with `status`, `message`, `retryAfter`, `quotaExhausted` and the raw
  `body`.
- Retries: per-minute rate limits are waited out (`Retry-After` when the runtime exposes
  it, otherwise `rateLimitRetryDelay`), transient failures of `GET` requests are retried
  with jittered backoff; writes are never repeated. An exhausted monthly quota is
  reported, not retried.
- Rate-limit information in `api.lastRateLimit` and `resp.rateLimit`.
- Pagination helpers for player search, history and the event log
  (`api.players.searchAll(...)`, `api.history.searchPages(...)`, ...).
- Optional Discord ban/unban fields on `players.ban` and `history.revoke`.
- Example resource with an ACE-protected `/txban` command (`examples/txapi-example`).
- Offline test suite (`tests/run.sh`) running under plain `lua5.4` with a mocked
  FiveM runtime.

### Not included

- WebSockets (`/ws/console`, `/ws/serverlog`): FiveM has no WebSocket client. Use
  `api.console.buffer()` / `api.serverlog.buffer()` for a REST snapshot.
- Webhook signature verification: FiveM's Lua runtime has no HMAC-SHA256. Receive
  webhooks with the Node, Python or Java SDK.
