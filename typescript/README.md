# @txapi/sdk

Official TypeScript / JavaScript SDK for the [txApi](https://txapi.cloud) REST and WebSocket API —
manage txAdmin / FiveM servers from your own code: search and ban players, run console commands,
stream the live console, read the event log, and verify webhooks.

- Typed: every endpoint has typed parameters and responses (TypeScript 5, `strict`).
- Zero runtime dependencies. Uses only `fetch`, `AbortSignal` and Web Crypto — runs on
  **Node.js ≥ 18, Deno, Bun, Cloudflare Workers / edge runtimes and browsers**.
- ESM **and** CommonJS, with `.d.ts` for both.
- Retries, rate-limit headers, auto-pagination, typed errors, live streams with reconnect.

API version: **1.8.0** (`API_VERSION`). SDK version: **1.0.0** (`VERSION`).

> **Never put your API key in a browser.** A key in client-side JavaScript is readable by
> everyone who opens the page and grants full access to your servers within its scopes.
> Use the SDK on a server (Node, Deno, Bun, a serverless function, a FiveM-side web service …)
> and expose only what your users need through your own endpoints.

## Contents

- [Install](#install)
- [Quickstart](#quickstart)
- [Configuration](#configuration)
- [Calling endpoints](#calling-endpoints)
- [Errors](#errors)
- [Retries](#retries)
- [Rate limits](#rate-limits)
- [Pagination](#pagination)
- [Live console and server log (WebSockets)](#live-console-and-server-log-websockets)
- [Webhooks](#webhooks)
- [Hooks: logging and metrics](#hooks-logging-and-metrics)
- [Endpoint reference](#endpoint-reference)
- [Examples](#examples)

## Install

```bash
npm install @txapi/sdk
# pnpm add @txapi/sdk · yarn add @txapi/sdk · bun add @txapi/sdk
```

Every release is also attached to
[GitHub Releases](https://github.com/txadmin-api/txapi-sdks/releases) and served from
`https://txapi.cloud/sdks/` (checksums in `SHA256SUMS`) — npm, pnpm, yarn and bun accept the
tarball URL directly, and the package name stays `@txapi/sdk`:

```bash
npm install https://txapi.cloud/sdks/txapi-sdk-1.0.0.tgz
```

## Quickstart

Create an API key in the dashboard (**Dashboard → API keys**). Then:

```ts
import { TxApi } from "@txapi/sdk";

const txapi = new TxApi({
  apiKey: process.env.TXAPI_KEY, // txp_…
  serverId: 1,                   // default server for every call
});

// Who's online?
const { players } = await txapi.players.online();
console.log(players.map((p) => p.displayName));

// Ban someone for 24 hours
await txapi.players.ban({
  identifier: "license:0123456789abcdef0123456789abcdef01234567",
  reason: "Cheating",
  durationHours: 24,
});
```

Your plan, quota and the key's permissions:

```ts
const me = await txapi.account.me();
console.log(me.plan.code, me.plan.features, me.quota.unlimited ? "∞" : me.quota.remaining);
```

## Configuration

```ts
const txapi = new TxApi({
  apiKey: "txp_…",                 // or env TXAPI_KEY — required
  baseUrl: "https://txapi.cloud",  // or env TXAPI_BASE_URL; "/api/v1" is added by the SDK
  serverId: 1,                     // default ?serverId= for server-scoped calls
  timeout: 30_000,                 // per attempt, milliseconds
  maxRetries: 2,                   // 0 disables automatic retries
  userAgent: "my-bot/2.1",         // appended: "txapi-typescript/1.0.0 my-bot/2.1"
  fetch: customFetch,              // e.g. a proxy-aware fetch or a test double
  WebSocket: WebSocketImpl,        // for live streams on Node 18/20 (see below)
  onRequest: (info) => {},         // before every attempt
  onResponse: (info) => {},        // after every attempt
});
```

- **Environment fallbacks** (`TXAPI_KEY`, `TXAPI_BASE_URL`) are read only where `process.env`
  exists (Node, Bun, Deno 2). In browsers and edge runtimes pass
  the values explicitly.
- A missing API key throws `ValidationError` immediately.
- `String(txapi)`, `JSON.stringify(txapi)` and `console.log(txapi)` never print the key.

### serverId

A key that is **bound to one server** needs no `serverId` at all. An account-wide key needs it
for every server-scoped call; set it once on the client or per call:

```ts
await txapi.server.restart({ serverId: 2 }); // overrides the client default
```

`account.me()` and `account.servers()` are account-level and never send `serverId`.

## Calling endpoints

Endpoints are grouped by resource: `account`, `players`, `history`, `server`
(+ `server.resources`), `console`, `serverlog`, `events`, `whitelist` (+ `whitelist.approvals`),
`admins`. Every method has TSDoc with a summary and the required permission.

Conventions:

- Path parameters come first: `txapi.history.revoke(actionId, { reason })`.
- Endpoints with a single body field take it positionally: `txapi.server.announce("Restart in 5")`.
- Endpoints with several fields take an object: `txapi.players.warn({ identifier, reason })`.
- `null` / `undefined` fields are left out of the request.
- The last argument is always optional request options:

```ts
const controller = new AbortController();
await txapi.players.search(
  { query: "john" },
  { serverId: 3, signal: controller.signal, timeout: 5_000, maxRetries: 0 },
);
```

Responses are typed but keep unknown fields — txAdmin passthrough responses are
`JsonObject`, and every typed response allows extra properties.

### Status, headers and rate limit of a single call

Every call returns an `ApiPromise`. Await it for the data, or call `.withResponse()`:

```ts
const { data, response } = await txapi.players.online().withResponse();
console.log(response.status, response.headers.get("content-type"), response.rateLimit);
```

### Client-side validation

Required fields must be present and non-blank, and string fields respect the API's
`maxLength` (reason/message 512, note 2000, resource name 128, console command 1024).
A violation throws `ValidationError` (with `.field`) **without sending anything**. Everything
else (enum values, number ranges) is validated by the server.

## Errors

Every error extends `TxApiError` (`status`, `body`, `headers`, `rateLimit`, `message`).
The message is the server's `error` text; the API key never appears in it.

| Status | Class | Meaning |
|---|---|---|
| 400, 405, 415 | `BadRequestError` | invalid input |
| 401 | `AuthenticationError` | key missing, invalid, revoked or expired |
| 403 | `PermissionDeniedError` | plan/key scope missing, IP not allowed, server disabled, txAdmin permission |
| 404 | `NotFoundError` | unknown player, action, admin or server |
| 409 | `ConflictError` | e.g. no server configured |
| 422 | `TxAdminRejectedError` | txAdmin refused the action (e.g. the player is offline) |
| 429 | `RateLimitError` | `retryAfter` (seconds or `null`); **`quotaExhausted`** = no `Retry-After` |
| 502 | `TxAdminUnavailableError` | txAdmin unreachable |
| other 5xx | `ServerError` | |
| — | `ConnectionError` | no response (DNS, refused, reset, TLS) |
| — | `TimeoutError` | the attempt exceeded `timeout` |
| — | `ValidationError` | client-side check failed, nothing was sent |
| — | `WebhookVerificationError` | webhook signature/timestamp invalid |
| — | `CommandRejectedError` | live console answered a command with `error` |
| — | `StreamClosedError` | live stream closed for good (`code`, `reason`) |

```ts
import { NotFoundError, RateLimitError, TxAdminRejectedError, TxApiError } from "@txapi/sdk";

try {
  await txapi.players.kick({ identifier: "license:abc", reason: "AFK" });
} catch (err) {
  if (err instanceof TxAdminRejectedError) {
    console.log("txAdmin said no:", err.message); // e.g. player offline
  } else if (err instanceof NotFoundError) {
    console.log("No such player");
  } else if (err instanceof RateLimitError) {
    if (err.quotaExhausted) console.log("Monthly quota used up — resets on the 1st (UTC)");
    else console.log(`Slow down, retry in ${err.retryAfter}s`);
  } else if (err instanceof TxApiError) {
    console.log(err.status, err.message, err.body);
  } else {
    throw err;
  }
}
```

Aborting via `signal` rejects with the signal's reason (an `AbortError` `DOMException` by
default), not with a `TxApiError`.

## Retries

Automatic retries (default `maxRetries: 2`, set `0` to disable — per client or per call):

- **429 with `Retry-After` ≤ 60 s** — retried for **any** method after the server's delay
  (the request was not executed).
- **429 without `Retry-After`** — the monthly quota is exhausted: **never** retried.
- **Connection errors, timeouts, 502, 503, 504** — retried for **GET only**. A retried POST
  could ban someone twice or run a command twice, so writes are never repeated.
- Backoff: `min(8 s, 0.5 s · 2^attempt)` with full jitter, unless `Retry-After` says otherwise.

## Rate limits

Every response carries `X-RateLimit-Limit` / `-Remaining` / `-Reset`. The SDK parses them into
`{ limit, remaining, resetSeconds }` (numbers or `null`):

```ts
await txapi.players.online();
console.log(txapi.lastRateLimit); // { limit: 120, remaining: 119, resetSeconds: 42 }
```

Also available per call (`.withResponse()` → `response.rateLimit`) and on every HTTP error
(`err.rateLimit`). The monthly quota is in `txapi.account.me()`.

## Pagination

List endpoints return one page. Each has an `…All` variant that fetches page after page
lazily (one request per page) and stops on the last page:

```ts
for await (const player of txapi.players.searchAll({ query: "john", sortBy: "tsJoined" })) {
  console.log(player.displayName);
}

// Whole pages
for await (const page of txapi.history.bansAll().pages()) {
  console.log(page.history.length);
}

// Collect (with an optional limit — stops fetching once reached)
const last50Kills = await txapi.events.killsAll({ since: "7d" }).toArray(50);
```

| Paginated method | Cursor |
|---|---|
| `players.searchAll`, `players.onlineAll`, `players.lookupAll` | `offset` = sort key of the last row (`playTime`, `tsJoined`, `tsLastConnection`; aliases `sessions` → `tsJoined`, `lastConnection` → `tsLastConnection`), `cursor` = its `license` |
| `history.bansAll`, `history.warnsAll`, `history.searchAll` | `offset` = `timestamp`, `cursor` = `id` of the last row |
| `events.listAll`, `deathsAll`, `killsAll`, `chatAll`, `commandsAll`, `adminActionsAll`, `actionsAll`, `sessionsAll` | `nextCursor` of the response |

Pass `{ signal }` as request options to abort an iteration. You can also page manually with
`offset` / `cursor` on the single-page methods.

Time parameters (`since`, `until`) accept ISO strings, epoch milliseconds, relative values
(`"24h"`, `"7d"`) or a `Date`.

## Live console and server log (WebSockets)

```ts
const live = txapi.console.connect(); // /ws/console, permission console.write

live.on("init", (lines) => console.log(`${lines.length} buffered lines`));
live.on("log", (line) => console.log(line)); // one call per line
live.on("reconnecting", ({ attempt, delayMs, code }) => console.warn("reconnecting", code));
live.on("error", (err) => console.warn("server:", err.message));
live.on("close", ({ code, reason, willReconnect }) => {});

await live.ready(); // resolves on the first `init` frame (the server accepted the stream)
const ack = await live.sendCommand("status"); // resolves with the server's ack
await live.close();
```

Or as an async iterator — one item per line, tagged with the frame it came from:

```ts
for await (const { type, line } of txapi.console.connect()) {
  if (type === "log") console.log(line); // type is "init" for the buffer, "log" for new lines
}
```

The server log works the same way (read-only, entries are objects or strings):

```ts
const log = txapi.serverlog.connect({ serverId: 2 }); // /ws/serverlog, permission logs.view
log.on("log", (entry) => console.log(entry));
```

**Behaviour**

- `sendCommand(cmd)` validates (non-blank, ≤ 1024 chars), sends, and resolves with the next
  `ack`. It rejects with `CommandRejectedError` on an `error` frame, `TimeoutError` after
  10 s (`commandTimeout` option), or `ConnectionError` when not connected or when the
  connection drops before the ack (the command may or may not have run). Acks are matched in
  order (FIFO). An `error` frame without a pending command is emitted as an `error` event.
- **Reconnects automatically** on close codes 1001, 1006, 1011, 1012 and network loss, with
  backoff 1 s → 30 s and jitter. After a reconnect the server sends `init` again (the full
  buffer) — de-duplicate or ignore it as you like.
- **Does not reconnect** on 1008 (bad key, missing permission, quota, client too slow) or 1009,
  or after `close()`. The stream then finishes: `ready()` rejects (if it never got going),
  iterators throw a `StreamClosedError` with the server's `reason`, and `closed()` resolves.
- Attach listeners / start iterating **before** awaiting `ready()`, otherwise the first `init`
  has already been delivered.
- Tune it: `connect({ reconnect: false })` or
  `connect({ reconnect: { initialDelayMs, maxDelayMs, maxAttempts } })`.
- The API key travels in the WebSocket URL's query string (the server reads it from there).
  The SDK never puts the URL into errors or events.

### Which WebSocket?

Node.js ≥ 22, Deno, Bun and browsers have a global `WebSocket`, used automatically.
On **Node.js 18 / 20**, install [`ws`](https://www.npmjs.com/package/ws) and pass it:

```ts
import { WebSocket } from "ws";
import { TxApi } from "@txapi/sdk";

const txapi = new TxApi({ apiKey: process.env.TXAPI_KEY, WebSocket });
// or per stream: txapi.console.connect({ WebSocket })
```

## Webhooks

txApi signs every delivery with `X-Txapi-Signature: t=<unix seconds>,v1=<hex>`
(HMAC-SHA256 of `"<t>.<raw body>"` with your `whsec_…` secret). `verifyWebhook` checks the
signature (any `v1` may match — secret rotation), checks the timestamp is within 300 s and
returns the parsed payload:

```ts
import { verifyWebhook, WebhookVerificationError } from "@txapi/sdk";

const event = await verifyWebhook(rawBody, signatureHeader, process.env.TXAPI_WEBHOOK_SECRET!);
// options: { toleranceSeconds: 300, now: Date | unix seconds }

switch (event.event) {
  case "player.banned":
    console.log(event.data);
    break;
  case "server.offline":
    // …
}
```

> **⚠️ Verify the RAW request body — the exact bytes you received.**
> **Do not `JSON.parse` the body and stringify it again** (and do not let a body-parser
> middleware do that before you): whitespace, key order and number formatting change, and the
> signature will never match. Pass the raw `string`, `Buffer`, `Uint8Array` or `ArrayBuffer`.

Payload: `{ id, event, createdAt, serverId, data }`. `id` is stable across retries — store it
to skip duplicates. Event names are in `WEBHOOK_EVENTS` (type `WebhookEvent`). The headers
`X-Txapi-Event` and `X-Txapi-Delivery` are exported as `EVENT_HEADER` / `DELIVERY_HEADER`.
Answer with a 2xx quickly; failed deliveries are retried.

**Express** — use `express.raw` on the webhook route, not `express.json`:

```ts
import express from "express";
import { verifyWebhook, WebhookVerificationError } from "@txapi/sdk";

const app = express();
app.post("/txapi", express.raw({ type: "application/json" }), async (req, res) => {
  try {
    const event = await verifyWebhook(req.body, req.header("X-Txapi-Signature") ?? "", secret);
    console.log(event.event, event.data);
    res.sendStatus(200);
  } catch (err) {
    if (err instanceof WebhookVerificationError) return res.status(400).send(err.message);
    throw err;
  }
});
```

**Next.js (App Router)** — `request.text()` gives you the raw body:

```ts
// app/api/txapi/route.ts
import { verifyWebhook } from "@txapi/sdk";

export async function POST(request: Request) {
  const raw = await request.text();
  try {
    const event = await verifyWebhook(raw, request.headers.get("x-txapi-signature") ?? "", process.env.TXAPI_WEBHOOK_SECRET!);
    console.log(event.event);
    return new Response("ok");
  } catch {
    return new Response("invalid signature", { status: 400 });
  }
}
```

**Hono** (Node, Bun, Cloudflare Workers):

```ts
import { Hono } from "hono";
import { verifyWebhook } from "@txapi/sdk";

const app = new Hono();
app.post("/txapi", async (c) => {
  const raw = await c.req.text();
  try {
    const event = await verifyWebhook(raw, c.req.header("x-txapi-signature") ?? "", c.env.TXAPI_WEBHOOK_SECRET);
    return c.text("ok");
  } catch {
    return c.text("invalid signature", 400);
  }
});
```

A framework-free `node:http` receiver is in [`examples/webhook-server.ts`](examples/webhook-server.ts).

## Hooks: logging and metrics

```ts
const txapi = new TxApi({
  onRequest: ({ endpoint, method, url, attempt }) => log.debug(`${method} ${url} (#${attempt})`),
  onResponse: ({ endpoint, status, durationMs, rateLimit, error }) =>
    metrics.timing(`txapi.${endpoint}`, durationMs, { status }),
});
```

`url` never contains the API key (it is sent as a header). Errors thrown inside a hook are
ignored — a hook can never break a request.

## Endpoint reference

| Method | HTTP | Permission |
|---|---|---|
| `account.me()` | `GET /api/v1/me` | any key |
| `account.servers()` | `GET /api/v1/servers` | any key |
| `players.search({ query, sortBy, order, filters, offset, cursor })` · `searchAll` | `GET /api/v1/players/search` | `players.read` |
| `players.online({ offset, cursor })` · `onlineAll` | `GET /api/v1/players/online` | `players.read` |
| `players.lookup(identifier, { offset, cursor })` · `lookupAll` | `GET /api/v1/players/lookup` | `players.read` |
| `players.info(identifier)` | `GET /api/v1/players/info` | `players.read` |
| `players.stats()` | `GET /api/v1/players/stats` | `players.read` |
| `players.warn({ identifier, reason })` | `POST /api/v1/players/warn` | `players.write` |
| `players.ban({ identifier, reason, durationHours, discordBan })` | `POST /api/v1/players/ban` | `players.write` |
| `players.kick({ identifier, reason })` | `POST /api/v1/players/kick` | `players.write` |
| `players.dm({ identifier, message })` | `POST /api/v1/players/dm` | `players.write` |
| `players.setNote({ identifier, note })` | `POST /api/v1/players/notes` | `players.write` |
| `players.setWhitelisted({ identifier, whitelisted })` | `POST /api/v1/players/whitelist` | `players.write` |
| `history.bans({ order, offset, cursor })` · `bansAll` | `GET /api/v1/history/bans` | `players.read` |
| `history.warns(…)` · `warnsAll` | `GET /api/v1/history/warns` | `players.read` |
| `history.search({ type, admin, searchType, search, … })` · `searchAll` | `GET /api/v1/history/search` | `players.read` |
| `history.banTemplates()` | `GET /api/v1/txadmin/ban-templates` | `players.read` |
| `history.action(actionId)` | `GET /api/v1/history/action/{actionId}` | `players.read` |
| `history.stats()` | `GET /api/v1/history/stats` | `players.read` |
| `history.revoke(actionId, { reason, discordUnban })` | `POST /api/v1/history/revoke/{actionId}` | `players.write` |
| `server.status()` | `GET /api/v1/server/status` | `server.control` |
| `server.diagnostics()` | `GET /api/v1/server/diagnostics` | `server.control` |
| `server.drops()` | `GET /api/v1/server/drops` | `server.control` |
| `server.performance({ thread })` | `GET /api/v1/server/performance` | `server.control` |
| `server.restart()` · `start()` · `stop()` | `POST /api/v1/server/{restart,start,stop}` | `server.control` |
| `server.scheduleRestart(time)` | `POST /api/v1/server/schedule-restart` | `server.control` |
| `server.skipRestart(skip)` | `POST /api/v1/server/skip-restart` | `server.control` |
| `server.kickAll(reason?)` | `POST /api/v1/server/kickall` | `server.control` |
| `server.announce(message)` | `POST /api/v1/server/announcement` | `server.control` |
| `server.resources.start(name)` · `stop` · `restart` · `ensure` | `POST /api/v1/server/resource/{start,stop,restart,ensure}` | `server.control` |
| `server.resources.refresh()` | `POST /api/v1/server/resource/refresh` | `server.control` |
| `console.buffer()` | `GET /api/v1/console` | `console.write` |
| `console.command(command)` | `POST /api/v1/console/command` | `console.write` |
| `console.connect(options)` | `WS /ws/console` | `console.write` |
| `serverlog.buffer()` | `GET /api/v1/server/log` | `logs.view` |
| `serverlog.history({ direction, ref })` | `GET /api/v1/server/log/history` | `logs.view` |
| `serverlog.connect(options)` | `WS /ws/serverlog` | `logs.view` |
| `events.list({ type, player, search, since, until, limit, cursor })` · `listAll` | `GET /api/v1/events` | `logs.view` |
| `events.deaths(…)` · `deathsAll` | `GET /api/v1/events/deaths` | `logs.view` |
| `events.kills(…)` · `killsAll` | `GET /api/v1/events/kills` | `logs.view` |
| `events.leaderboard({ since, sort, limit })` | `GET /api/v1/events/leaderboard` | `logs.view` |
| `events.player(player, { since })` | `GET /api/v1/events/player` | `logs.view` |
| `events.summary({ since })` | `GET /api/v1/events/summary` | `logs.view` |
| `events.chat(…)` · `chatAll` | `GET /api/v1/events/chat` | `logs.view` |
| `events.commands(…)` · `commandsAll` | `GET /api/v1/events/commands` | `logs.view` |
| `events.adminActions(…)` · `adminActionsAll` | `GET /api/v1/events/admin-actions` | `logs.view` |
| `events.actions(…)` · `actionsAll` | `GET /api/v1/events/actions` | `logs.view` |
| `events.sessions(…)` · `sessionsAll` | `GET /api/v1/events/sessions` | `logs.view` |
| `events.playtime(…)` | `GET /api/v1/events/playtime` | `logs.view` |
| `events.activity(…)` | `GET /api/v1/events/activity` | `logs.view` |
| `events.actionLog({ lines })` | `GET /api/v1/txadmin/action-log` | `logs.view` |
| `whitelist.requests({ page })` | `GET /api/v1/whitelist/requests` | `players.read` |
| `whitelist.approve(requestId)` · `deny(requestId)` | `POST /api/v1/whitelist/requests/{requestId}/{approve,deny}` | `players.write` |
| `whitelist.denyAll(before?)` | `POST /api/v1/whitelist/requests/deny-all` | `players.write` |
| `whitelist.approvals.list()` | `GET /api/v1/whitelist/approvals` | `players.read` |
| `whitelist.approvals.add(identifier)` | `POST /api/v1/whitelist/approvals` | `players.write` |
| `whitelist.approvals.remove(identifier)` | `DELETE /api/v1/whitelist/approvals/{identifier}` | `players.write` |
| `admins.list()` | `GET /api/v1/txadmin/admins` | `admin.manage` |
| `admins.get(name)` | `GET /api/v1/txadmin/admins/{name}` | `admin.manage` |
| `admins.availablePermissions()` | `GET /api/v1/txadmin/admins/available-permissions` | `admin.manage` |
| `admins.add({ name, permissions, citizenfxId, discordId })` | `POST /api/v1/txadmin/admins` | `admin.manage` |
| `admins.edit(name, { permissions, citizenfxId, discordId })` | `PUT /api/v1/txadmin/admins/{name}` | `admin.manage` |
| `admins.delete(name)` | `DELETE /api/v1/txadmin/admins/{name}` | `admin.manage` |

The full API documentation, including response shapes, is at <https://txapi.cloud/docs>; the
OpenAPI document is at <https://txapi.cloud/openapi.json>.

## Examples

Runnable scripts in [`examples/`](examples) (after `npm run build` in this package, or with the
package installed):

```bash
TXAPI_KEY=txp_… TXAPI_SERVER_ID=1 node --experimental-strip-types examples/quickstart-ban.ts "john" "cheating"
TXAPI_KEY=txp_… TXAPI_SERVER_ID=1 node --experimental-strip-types examples/live-console.ts "status"
TXAPI_WEBHOOK_SECRET=whsec_… node --experimental-strip-types examples/webhook-server.ts
# or: npx tsx examples/quickstart-ban.ts
```

## Development

```bash
npm ci
npm run typecheck   # tsc --noEmit
npm test            # vitest, fully offline (local mock servers)
npm run build       # tsup → dist/ (ESM + CJS + .d.ts)
```

## License

MIT © 2026 Josef Peter
