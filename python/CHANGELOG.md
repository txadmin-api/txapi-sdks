# Changelog

All notable changes to the txApi Python SDK (distribution `txapi-sdk`, import `txapi`). The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versions follow
[Semantic Versioning](https://semver.org/).

## [1.0.0] — 2026-10-14

First release, targeting txApi **1.8.0**. Published on PyPI as **`txapi-sdk`** (the name
`txapi` there is taken); the import name is `txapi`.

### Added

- `TxApi` (sync) and `AsyncTxApi` (async) clients with an identical surface, built on `httpx`;
  configuration from arguments or `TXAPI_KEY` / `TXAPI_BASE_URL`, default `server_id`,
  per-call `server_id=` and `timeout=`, custom `http_client=`, `on_request` / `on_response` hooks.
- One method for each of the 67 endpoints (`account`, `players`, `history`, `server`,
  `server.resources`, `console`, `serverlog`, `events`, `whitelist`, `whitelist.approvals`,
  `admins`), fully type-hinted with `TypedDict` responses (`txapi.types`), docstrings naming
  the required permission.
- Discord ban/unban flags: `players.ban(..., discord_ban=True)` and
  `history.revoke(..., discord_unban=True)` with the `discord` result.
- Client-side validation (required fields, maximum lengths) before any request.
- Error hierarchy (`TxApiError` and one subclass per status, `RateLimitError.quota_exhausted`,
  `APIConnectionError`, `APITimeoutError`, `ValidationError`, `WebhookVerificationError`,
  `CommandError`); the API key never appears in `repr`/`str`.
- Retries: 429 with `Retry-After` ≤ 60 s for every method; network errors and 502/503/504 for
  GET only; full-jitter backoff; `max_retries=0` disables retries.
- Rate-limit information (`client.last_rate_limit`, `error.rate_limit`) and raw responses via
  `client.with_raw_response`.
- Lazy auto-paging iterators (`iter_*`, sync and async) for players, history and events.
- `verify_webhook` with constant-time HMAC-SHA256 check, timestamp tolerance and secret
  rotation; `WEBHOOK_EVENTS` and the `WebhookEventName` literal type.
- Live console and server-log streams (`txapi[live]`, `websockets`): async iteration or
  callbacks, `send_command` with FIFO ack correlation and timeout, automatic reconnect on
  1001/1006/1011/1012 and network loss, no reconnect on 1008/1009 with the close reason surfaced.
