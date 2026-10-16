# txApi SDK contract

Every official SDK (`typescript/`, `python/`, `java/`, `fivem/`) implements
the behaviour below **identically**. When the API changes, this file, the
generated `spec/endpoints.json` and every SDK change in the same commit.

Sources of truth, in this order:

1. The txApi API catalog — endpoints, parameters, body fields, permissions,
   response examples, WebSocket frames and close codes. It lives in the
   txApi main repository; its public form is <https://txapi.cloud/openapi.json>
   and <https://txapi.cloud/docs>.
2. `spec/endpoints.json` — generated from (1). Never edit by hand.
3. `spec/webhook-vectors.json` — shared signature test vectors.
4. The API's implementation when (1) leaves a detail open.

API version targeted by SDK 1.0.0: **1.8.0** (`spec/endpoints.json → apiVersion`).
Each SDK exposes it as a constant (`API_VERSION`) next to its own `VERSION`.

---

## 1. Client configuration

| Option | Default | Notes |
|---|---|---|
| `apiKey` | env `TXAPI_KEY` | required; missing → error at construction, never a request without a key |
| `baseUrl` | env `TXAPI_BASE_URL`, else `https://txapi.cloud` | trailing slash stripped; `/api/v1` is appended by the SDK |
| `serverId` | none | default server for every server-scoped call; every call can override it |
| `timeout` | 30 s | per attempt |
| `maxRetries` | 2 | see §5 |
| `userAgent` | — | appended to `txapi-<lang>/<version>` |
| transport injection | — | TS: `fetch`; Python: `httpx.Client`/`AsyncClient`; Java: `HttpClient` |
| hooks | — | `onRequest` / `onResponse` (for logging/metrics); must not be able to break a request |

The key is sent as `Authorization: Bearer <key>`. Never log it; `toString`/`repr`
of the client and of errors must not contain it.

## 2. Request building

- Path parameters are percent-encoded (identifiers contain `:`; admin names may contain spaces).
- `serverId` is added as query parameter to every endpoint **except** those with
  `accountLevel: true` (`/me`, `/servers`). Per-call value wins over the client default.
- Query: omit `null`/absent values, booleans as `true`/`false`.
- JSON bodies: **omit absent fields, never send `null`**. This is semantic, not cosmetic:
  `PUT /txadmin/admins/{name}` keeps every field that is not sent; `whitelisted` must be explicit.
- Client-side validation before any network call (throws the SDK's validation error):
  required body fields present and non-blank, `maxLength` from the spec. Nothing more —
  the server stays the authority for everything else.

## 3. Methods

Grouped by the first segment of the endpoint `id` (`players`, `history`, `server`,
`console`, `serverlog`, `events`, `whitelist`, `admins`, `account`). One method per
endpoint, named idiomatically for the language, e.g. `players.ban`, `history.revoke`,
`server.resources.restart` (resource endpoints may live in a nested group),
`whitelist.approvals.add`, `events.adminActions`, `account.me`.

Every one of the 67 endpoints in `spec/endpoints.json` has exactly one method.
Each SDK has a **coverage test** that calls every method against a recording mock
server and asserts the recorded `METHOD path` set equals `spec/endpoints.json`
(path templates matched, query string ignored). Adding an endpoint to the API
therefore breaks every SDK's tests until it is implemented.

Responses are returned as parsed JSON, typed after the catalog's `responseExample`
(typed models, but unknown extra fields must never break parsing — the API adds fields
in minor versions). Mutations return `{ success, message, discord? }`.

### Discord ban/unban (API 1.8.0)
`players.ban({ identifier, reason, durationHours?, discordBan? })` and
`history.revoke(actionId, { reason?, discordUnban? })` return an optional
`discord` result: `{ discordId?, skipped?, message?, guilds?: [{ guildId, guildName, ok, code, message? }] }`.

## 4. Errors

The API answers errors as `{ "success": false, "error": "<text>" }` (sometimes just
`{ "error": … }`, or plain text from a proxy). One base error type with `status`,
`message` (the `error` text, else the body, else the reason phrase), parsed `body`,
response `headers` and the `rateLimit` (§6). Subtypes by status:

| Status | Error | Meaning |
|---|---|---|
| 400, 405, 415 | `BadRequestError` | invalid input |
| 401 | `AuthenticationError` | key missing, invalid, revoked or expired |
| 403 | `PermissionDeniedError` | plan feature / key scope missing, IP not allowed, server disabled, txAdmin account lacks a permission |
| 404 | `NotFoundError` | unknown player/action/admin/server |
| 409 | `ConflictError` | e.g. no server configured |
| 422 | `TxAdminRejectedError` | txAdmin refused the action (e.g. player offline) |
| 429 | `RateLimitError` | `retryAfter` (seconds or null); **`quotaExhausted` = no `Retry-After`** (monthly quota, resets on the 1st, UTC) |
| 502 | `TxAdminUnavailableError` | txAdmin unreachable or answered garbage |
| other 5xx | `ServerError` | |
| — | `ConnectionError`, `TimeoutError` | transport problems |
| — | `ValidationError` | client-side check from §2 (no request was sent) |
| — | `WebhookVerificationError` | §8 |

## 5. Retries

- 429 **with** `Retry-After` ≤ 60 s: retry any method after that delay (the request was
  not executed). 429 without `Retry-After` (quota): never retry.
- Connection errors, timeouts, 502, 503, 504: retry **GET only** — a retried POST could
  ban twice or restart a server twice.
- Backoff: `min(8 s, 0.5 s · 2^attempt)` with full jitter, unless `Retry-After` says otherwise.
- `maxRetries: 0` disables everything. Cancellation (AbortSignal / interrupt / task cancel)
  stops waiting immediately.

## 6. Rate-limit information

Every response carries `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
(seconds until reset). Parse into `RateLimit { limit, remaining, resetSeconds }`, expose the
last one on the client and on errors. Each SDK also offers a way to get the raw response
(status, headers, rateLimit) together with the parsed data.

## 7. Pagination

Auto-paging iterators that fetch the next page lazily (and respect cancellation):

| Endpoints | Page shape | Next page |
|---|---|---|
| `players.search`, `players.online`, `players.lookup` | `{ players[], hasReachedEnd }` | `offset` = last row's value of the sort key (`playTime` default, `tsJoined`, `tsLastConnection`; aliases `sessions`→`tsJoined`, `lastConnection`→`tsLastConnection`; online/lookup sort by `playTime`), `cursor` = last row's `license` |
| `history.bans`, `history.warns`, `history.search` | `{ history[], hasReachedEnd }` | `offset` = last row's `timestamp`, `cursor` = last row's `id` |
| `events.list`, `deaths`, `kills`, `chat`, `commands`, `adminActions`, `actions`, `sessions` | `{ events[] (or the list field in the example), nextCursor? }` | `cursor` = `nextCursor`; stop when it is absent |

Stop on `hasReachedEnd`, a missing `nextCursor`, or an empty page. Verify the exact list
field names against the `responseExample`s in the catalog.

## 8. Webhooks

`verifyWebhook(rawBody, signatureHeader, secret, { toleranceSeconds = 300, now? })`:

- Header `X-Txapi-Signature: t=<unix seconds>,v1=<hex>` — accept several `v1` entries
  (valid if any matches).
- Expected = hex HMAC-SHA256(key = secret as UTF-8, message = `"<t>.<rawBody>"`).
- Constant-time comparison; reject if `|now − t| > tolerance`.
- On success return the parsed event `{ id, event, createdAt, serverId, data }`;
  otherwise throw `WebhookVerificationError` with a reason.
- **Must work on the raw body bytes** — re-serialising parsed JSON breaks the signature;
  the README says so in bold.
- Export the list of event names (`spec/endpoints.json → webhookEvents`) as constants.
- Other headers: `X-Txapi-Event`, `X-Txapi-Delivery`. Test: `spec/webhook-vectors.json`.

## 9. Live streams (WebSocket)

`/ws/console` (permission `console.write`) and `/ws/serverlog` (`logs.view`).
URL = baseUrl with `http→ws`/`https→wss` + path + `?apiKey=<key>[&serverId=<id>]`
(query auth: the server reads the key from the query string).

Server frames: console `init {lines[]}`, `log {lines[]}`, `ack {message}`, `error {message}`;
serverlog `init {entries[]}`, `log {entries[]}` (entries are objects or strings), `error {message}`.
Client frame (console only): `{ "type": "command", "command": "<≤1024 chars>" }`.

- Expose events/callbacks or an async stream: `init`, `log` (one call per line/entry
  is the most useful default; raw frames also available), `error`, `open`, `close`, `reconnecting`.
- `sendCommand(cmd)` validates length, sends, and resolves with the next `ack`
  (or rejects with the next `error`) — FIFO correlation, 10 s timeout. An `error`
  frame with no pending command is emitted as an event.
- **Reconnect** automatically on 1001, 1006 (abnormal), 1011, 1012 and network loss:
  backoff 1 s → 30 s with jitter. **Do not** reconnect on 1008 (auth/permission/quota/too
  slow — the close reason is surfaced to the user) or 1009, or after the user called `close()`.
- After a reconnect the server sends `init` again (full buffer); emit it as `init`
  so users can de-duplicate or ignore it.

## 10. Quality bar

- Typed public API, documented (TSDoc / docstrings / Javadoc) with the permission each
  method needs. No `any`/untyped escape hatches in the public surface except raw JSON
  (`unknown`/`Any`/`JsonNode`) for txAdmin passthrough endpoints.
- Zero or minimal runtime dependencies (documented per SDK).
- Tests run offline against local mock HTTP/WS servers: coverage test (§3), request
  building (§2), error mapping (§4), retries (§5), rate limit (§6), pagination (§7),
  webhook vectors (§8), WebSocket reconnect/command correlation (§9).
- README: install, quickstart, every feature with an example, error handling, FiveM notes
  where relevant. `CHANGELOG.md` starting at 1.0.0. `examples/` with runnable scripts.
- License: MIT (`LICENSE` in each SDK).
