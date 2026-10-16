# Changelog

All notable changes to `@txapi/sdk` are documented here. This project follows
[Semantic Versioning](https://semver.org/).

## 1.0.0 — 2026-10-14

First release, targeting txApi **API version 1.8.0**.

- One typed method for each of the 67 REST endpoints, grouped into `account`, `players`,
  `history`, `server` (+ `server.resources`), `console`, `serverlog`, `events`, `whitelist`
  (+ `whitelist.approvals`) and `admins`.
- Client options: `apiKey`, `baseUrl`, `serverId`, `timeout`, `maxRetries`, `userAgent`,
  `fetch`, `WebSocket`, `onRequest`, `onResponse`; `TXAPI_KEY` / `TXAPI_BASE_URL` fallbacks.
- Per-call options: `serverId`, `signal`, `timeout`, `maxRetries`; `.withResponse()` for
  status, headers and rate limit.
- Typed error hierarchy (`TxApiError` and subclasses), `RateLimitError.quotaExhausted`.
- Automatic retries: 429 with `Retry-After` (any method), connection errors / timeouts /
  502 / 503 / 504 (GET only), exponential backoff with full jitter.
- Rate-limit headers parsed into `client.lastRateLimit`.
- Auto-pagination (`…All()` async iterators, `.pages()`, `.toArray(limit)`) for players,
  history and event lists.
- `verifyWebhook()` (Web Crypto, constant-time compare, secret rotation, timestamp tolerance),
  `WEBHOOK_EVENTS`, typed `WebhookPayload`.
- Live console and server log over WebSocket: typed events, async iteration,
  `sendCommand()` with FIFO ack correlation and timeout, automatic reconnect with backoff.
- ESM + CommonJS builds with type declarations; zero runtime dependencies; Node.js ≥ 18,
  Deno, Bun, edge runtimes and browsers.
