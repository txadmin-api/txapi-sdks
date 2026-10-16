# Changelog

All notable changes to the txApi Java SDK. The project follows [Semantic Versioning](https://semver.org).

## 1.0.0 — 2026-10-14

First release, built against txApi API **1.8.0**.

### Added

- `TxApi` client with builder and `TxApi.fromEnv()` (`TXAPI_KEY`, `TXAPI_BASE_URL`,
  `TXAPI_SERVER_ID`); Bearer authentication, base URL normalisation, the key masked in `toString()`.
- One typed method for each of the 67 endpoints, grouped as `account`, `players`, `history`,
  `server` (+ `resources`), `console`, `serverLog`, `events`, `whitelist` (+ `approvals`) and
  `admins`; every method also takes per-call `RequestOptions`.
- Asynchronous API via `client.async()` — the same groups returning `CompletableFuture`s.
- Server selection per call, per view (`forServer`) or as client default; account endpoints never
  send `serverId`.
- Local validation of required values and documented maximum lengths (`ValidationException`).
- Typed exception hierarchy per HTTP status, plus connection, timeout, cancellation, command and
  webhook-verification errors.
- Automatic retries with full-jitter backoff: 429 with `Retry-After` (any method), 502/503/504 and
  transport errors (GET only); never for an exhausted monthly quota.
- Rate-limit information on every response (`lastRateLimit()`, `withRawResponse`).
- Lazy auto-pagination (`…All` methods → `PageIterable` / `AsyncPageIterable`, with `stream()`
  and `pages()`), using the API's cursors for players, history and the event log.
- Live console (`/ws/console`) with acknowledged commands and live server log (`/ws/serverlog`),
  both reconnecting automatically.
- Webhook verification (`Webhooks.verify`) with timestamp tolerance and multiple signatures,
  `Webhooks.sign` for testing, and constants for every event name.
- Request/response hooks for logging and metrics.
- JPMS module `cloud.txapi.sdk`; the only runtime dependency is `jackson-databind`.
