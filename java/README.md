# txApi Java SDK

The official Java client for the [txApi](https://txapi.cloud) REST and WebSocket API — manage
FiveM servers through txAdmin from your own code: players, bans, warnings, whitelist, console,
server control, the event log and webhooks.

- **Every endpoint** of txApi API 1.8.0 (67 endpoints) as a typed method, blocking and asynchronous.
- **Typed results** (Java records), **typed errors** (one exception class per status), and the raw
  JSON when you need it.
- **Automatic retries** with backoff for rate limits and transient failures, **rate-limit info**
  on every response.
- **Pagination** as plain `Iterable`/`Stream`, fetched lazily.
- **Live console and server log** over WebSocket with automatic reconnect and acknowledged commands.
- **Webhook signature verification** in one call.
- Java 17+, JPMS module `cloud.txapi.sdk`, `java.net.http` only. **One runtime dependency:**
  `jackson-databind`.

```java
try (TxApi client = TxApi.builder().apiKey("txp_…").serverId(12).build()) {
    client.players().ban(BanRequest.of("license:abc123", "Cheating").withDurationHours(24));
}
```

---

## Contents

- [Installation](#installation)
- [Quickstart](#quickstart)
- [Configuration](#configuration)
- [The API at a glance](#the-api-at-a-glance)
- [Choosing the server](#choosing-the-server)
- [Errors](#errors)
- [Retries and timeouts](#retries-and-timeouts)
- [Rate limits and the raw response](#rate-limits-and-the-raw-response)
- [Pagination](#pagination)
- [Asynchronous API](#asynchronous-api)
- [Live console and server log](#live-console-and-server-log)
- [Webhooks](#webhooks)
- [Logging and hooks](#logging-and-hooks)
- [Thread safety and lifecycle](#thread-safety-and-lifecycle)
- [Examples](#examples)
- [Building from source](#building-from-source)

---

## Installation

From Maven Central (JDK 17+):

**Maven**

```xml
<dependency>
  <groupId>cloud.txapi</groupId>
  <artifactId>txapi-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

**Gradle (Kotlin DSL)**

```kotlin
dependencies {
    implementation("cloud.txapi:txapi-sdk:1.0.0")
}
```

**Gradle (Groovy DSL)**

```groovy
dependencies { implementation 'cloud.txapi:txapi-sdk:1.0.0' }
```

The one runtime dependency (`jackson-databind`) comes along transitively.

Without access to Maven Central: every release is also attached to
[GitHub Releases](https://github.com/txadmin-api/txapi-sdks/releases) and served from
`https://txapi.cloud/sdks/` (checksums in `SHA256SUMS`). Install the jar and its pom into your
local repository once, then add `mavenLocal()` to your Gradle repositories:

```bash
curl -fLO https://txapi.cloud/sdks/txapi-sdk-1.0.0.jar
curl -fLO https://txapi.cloud/sdks/txapi-sdk-1.0.0.pom
mvn install:install-file -Dfile=txapi-sdk-1.0.0.jar -DpomFile=txapi-sdk-1.0.0.pom
```

If your application is modular, add `requires cloud.txapi.sdk;` to your `module-info.java`
(it brings `com.fasterxml.jackson.databind` and `java.net.http` along transitively).

---

## Quickstart

Create an API key in the dashboard (Dashboard → API keys). The key decides what you may do: its
scopes and your plan's features are intersected, and a key can be bound to one server.

```java
import cloud.txapi.sdk.TxApi;
import cloud.txapi.sdk.model.*;
import cloud.txapi.sdk.params.*;

public class Main {
    public static void main(String[] args) {
        try (TxApi client = TxApi.builder()
                .apiKey(System.getenv("TXAPI_KEY"))
                .serverId(12)                      // optional with one server or a server-bound key
                .build()) {

            AccountInfo me = client.account().me();
            System.out.println("Plan: " + me.plan().code());

            PlayerPage page = client.players().search(PlayerSearchParams.create().query("john"));
            for (Player p : page.players()) {
                System.out.println(p.displayName() + " — " + p.license());
            }

            ActionResult r = client.players().ban(
                    BanRequest.of("license:abc123", "Cheating").withDurationHours(24));
            System.out.println(r.message());

            client.server().announce("Server restart in 5 minutes");
        }
    }
}
```

---

## Configuration

```java
TxApi client = TxApi.builder()
        .apiKey("txp_…")                 // required
        .baseUrl("https://txapi.cloud")       // default; "/api/v1" is appended for you
        .serverId(12)                         // default server for every call
        .timeout(Duration.ofSeconds(30))      // per attempt (default 30 s)
        .maxRetries(2)                        // default 2, 0 disables retries
        .userAgent("my-bot/1.4")              // appended to "txapi-java/1.0.0"
        .httpClient(myHttpClient)             // bring your own java.net.http.HttpClient
        .onRequest(e -> log.debug("→ {} {}", e.method(), e.uri()))
        .onResponse(e -> log.debug("← {} in {}", e.status(), e.elapsed()))
        .build();
```

| Option | Default | Notes |
|---|---|---|
| `apiKey` | env `TXAPI_KEY` | required — building without a key throws `IllegalArgumentException`, no request is ever sent without one |
| `baseUrl` | env `TXAPI_BASE_URL`, else `https://txapi.cloud` | trailing slash and a pasted `/api/v1` are stripped |
| `serverId` | env `TXAPI_SERVER_ID` | see [Choosing the server](#choosing-the-server) |
| `timeout` | 30 s | per attempt |
| `maxRetries` | 2 | see [Retries](#retries-and-timeouts) |

`TxApi.fromEnv()` builds a client from the three environment variables alone. The key is never
printed: `client.toString()` shows it as `apiKey=***`.

---

## The API at a glance

Methods are grouped like the [API reference](https://txapi.cloud/docs). Each method has an
overload with a trailing `RequestOptions` for per-call settings.

| Group | Methods |
|---|---|
| `account()` | `me()`, `servers()` |
| `players()` | `search`, `online`, `lookup`, `info`, `stats`, `warn`, `ban`, `kick`, `dm`, `setNote`, `setWhitelisted` (+ `searchAll`, `onlineAll`, `lookupAll`) |
| `history()` | `bans`, `warns`, `search`, `stats`, `action`, `revoke`, `banTemplates` (+ `bansAll`, `warnsAll`, `searchAll`) |
| `server()` | `status`, `diagnostics`, `performance`, `drops`, `restart`, `stop`, `start`, `kickAll`, `announce`, `scheduleRestart`, `skipRestart` |
| `server().resources()` | `refresh`, `start`, `stop`, `restart`, `ensure` |
| `console()` | `buffer()`, `command(cmd)`, `connect(listener)` |
| `serverLog()` | `buffer()`, `history(...)`, `connect(listener)` |
| `events()` | `list`, `deaths`, `kills`, `chat`, `commands`, `adminActions`, `actions`, `sessions`, `leaderboard`, `player`, `playtime`, `activity`, `summary`, `actionLog` (+ `…All` variants) |
| `whitelist()` | `requests`, `approve`, `deny`, `denyAll`, `approvals().list/add/remove` |
| `admins()` | `list`, `get`, `add`, `edit`, `delete`, `permissions` |

Some typical calls:

```java
// Players
client.players().warn("license:abc123", "Please read the rules");
client.players().kick("discord:123456789012345678", "AFK");
client.players().dm("license:abc123", "Your ticket was answered");
client.players().setNote("license:abc123", "Known troublemaker");
PlayerPage online = client.players().online();

// Search with filters and sorting
PlayerPage admins = client.players().search(PlayerSearchParams.create()
        .filters(PlayerFilter.ONLINE, PlayerFilter.ADMIN)
        .sortBy(PlayerSort.PLAY_TIME)
        .order(SortOrder.DESC));

// Bans: revoke, optionally lifting a Discord ban too
client.history().revoke("B1234", RevokeRequest.reason("Appeal accepted").withDiscordUnban(true));

// Ban on txAdmin AND on every Discord server linked through txBot
ActionResult r = client.players().ban(BanRequest.of("license:abc123", "Cheating").withDiscordBan(true));
r.discordResult().ifPresent(d -> System.out.println(d));

// Server control
client.server().scheduleRestart("+15");             // in 15 minutes ("HH:MM" = host clock)
client.server().resources().restart("my_resource");
ServerStatus status = client.server().status();      // status.online() = FXServer running

// Event log
Leaderboard top = client.events().leaderboard(LeaderboardQuery.create()
        .since(Duration.ofDays(7)).sort(LeaderboardSort.KILLS).limit(10));
EventPage chat = client.events().chat(ChatQuery.create().since(Duration.ofHours(1)));
```

Identifiers are typed strings like `license:…`, `discord:…`, `fivem:…`, `steam:…`; a bare
40-character hex string is treated as a license by the API.

Input is validated **before** anything is sent: missing required values and over-long texts
(reason 512, message 512, note 2000, resource name 128, command 1024 characters) throw a
`ValidationException` that names the field.

Responses with no stable shape in the API (`players().info`, `players().stats`,
`server().diagnostics`, …) are returned as Jackson `JsonNode`.

---

## Choosing the server

Most endpoints act on one txAdmin server; the SDK sends `serverId` as a query parameter. Account
endpoints (`account().me()`, `account().servers()`) never carry one.

Resolution order, most specific first:

1. `RequestOptions` of the call: `client.players().kick(id, RequestOptions.serverId(7))`
2. A server view: `client.forServer(7).players().kick(id)`
3. The client default (`builder().serverId(...)` or `TXAPI_SERVER_ID`)
4. None — fine if your account has exactly one server or the key is bound to one server.

```java
TxApi lobby = client.forServer(7);       // shares the connection pool, nothing to close
TxApi main  = client.forServer(12);
lobby.server().announce("…");
```

`withOptions(...)` creates a view with other defaults (timeout, retries, server) the same way.

---

## Errors

Every exception extends `cloud.txapi.sdk.errors.TxApiException` (unchecked).

| Status | Exception | Typical cause |
|---|---|---|
| 400, 405, 415 | `BadRequestException` | invalid parameter or body |
| 401 | `AuthenticationException` | key missing, unknown, revoked or expired |
| 403 | `PermissionDeniedException` | plan feature or key scope missing, IP not allowed, server disabled |
| 404 | `NotFoundException` | unknown player, action, admin or server |
| 409 | `ConflictException` | no server selected and the account has several |
| 422 | `TxAdminRejectedException` | txAdmin refused the action (player offline, …) |
| 429 | `RateLimitException` | too many requests per minute, or the monthly quota is used up |
| 502 | `TxAdminUnavailableException` | txApi could not reach your txAdmin |
| other 5xx | `ServerException` | |
| other 4xx | `ApiException` | |
| — | `ApiConnectionException` | network failure, DNS, TLS |
| — | `ApiTimeoutException` | the request timed out |
| — | `RequestCancelledException` | the thread was interrupted / the future cancelled |
| — | `ValidationException` | rejected locally before sending (`field()` names the input) |
| — | `CommandException` | a live-console command was rejected or timed out |
| — | `WebhookVerificationException` | a webhook delivery failed verification |

All HTTP errors are `ApiException`s and carry `status()`, the API's message as `getMessage()`,
the parsed `body()` and `rawBody()`, `headers()` and `rateLimit()`.

```java
try {
    client.players().ban(BanRequest.of(identifier, "Cheating"));
} catch (NotFoundException e) {
    reply("No such player.");
} catch (TxAdminRejectedException e) {
    reply("txAdmin said: " + e.getMessage());
} catch (RateLimitException e) {
    if (e.isQuotaExhausted()) {
        alertAdmins("Monthly API quota used up");      // waiting does not help
    } else {
        retryLater(e.retryAfter().orElse(Duration.ofSeconds(5)));
    }
} catch (TxApiException e) {
    log.error("txApi call failed", e);
}
```

`isQuotaExhausted()` is `true` for a 429 **without** `Retry-After`: the monthly quota is empty and
retrying is pointless until it resets (`client.account().me().quota().resetsAt()`).

---

## Retries and timeouts

The SDK retries by itself, with full-jitter exponential backoff
(`random(0, min(8 s, 0.5 s · 2^attempt))`):

| Situation | Retried? |
|---|---|
| 429 with `Retry-After` ≤ 60 s | yes, any method, after exactly `Retry-After` |
| 429 without `Retry-After` (quota exhausted) | **never** |
| 502 / 503 / 504, connection error, timeout | **GET only** — a write may already have happened (a ban is not idempotent) |
| everything else | never |

Configure it per client or per call:

```java
TxApi noRetry = client.withOptions(RequestOptions.builder().maxRetries(0).build());

client.server().restart(RequestOptions.builder()
        .timeout(Duration.ofSeconds(60))
        .maxRetries(0)
        .build());
```

`timeout` applies to each attempt. Interrupting the calling thread aborts the request and any
backoff wait with `RequestCancelledException` (the interrupt flag stays set).

---

## Rate limits and the raw response

Every response carries `X-RateLimit-Limit`, `X-RateLimit-Remaining` and `X-RateLimit-Reset`.
The client remembers the latest values:

```java
client.players().online();
client.lastRateLimit().ifPresent(rl ->
        System.out.println(rl.remaining() + "/" + rl.limit() + ", resets in " + rl.resetSeconds() + " s"));
```

For status code and headers of one specific call, wrap it in `withRawResponse`:

```java
ApiResponse<ServerStatus> res = client.withRawResponse(c -> c.server().status());
System.out.println(res.status() + " " + res.rateLimit() + " " + res.data().online());
```

---

## Pagination

List endpoints come in two forms: the plain method returns one page (with the cursor values you
need to continue), and the `…All` variant returns a lazy `PageIterable` that fetches the next page
only when you iterate past the current one.

```java
for (Player p : client.players().searchAll(PlayerSearchParams.create().query("john"))) {
    System.out.println(p.displayName());
}

List<String> lastBans = client.history().bansAll().stream()
        .limit(500)                         // stops fetching after 500 rows
        .map(HistoryEntry::playerName)
        .toList();

for (List<ServerEvent> page : client.events().deathsAll(DeathQuery.create().since(Duration.ofDays(1))).pages()) {
    store(page);                            // page by page
}
```

The SDK builds the cursors for you (players: sort-key value + license; history: timestamp + action
ID; events: `nextCursor`) and stops when the API reports the end, on an empty page, or if a cursor
would repeat. If you pass a cursor yourself, iteration starts from there.

---

## Asynchronous API

`client.async()` exposes the same groups with every method returning a `CompletableFuture`. It
shares the client's configuration and connection pool; retries wait on a scheduler, never with
`Thread.sleep`.

```java
TxApiAsync api = client.async();

api.players().ban(BanRequest.of("license:abc123", "Cheating"))
   .thenAccept(r -> System.out.println(r.message()))
   .exceptionally(e -> { log.error("ban failed", e); return null; });

CompletableFuture<ServerStatus> a = api.forServer(7).server().status();
CompletableFuture<ServerStatus> b = api.forServer(12).server().status();
CompletableFuture.allOf(a, b).join();

api.history().bansAll().toList(100).thenAccept(this::show);
```

Failures complete the future exceptionally with the same exception types (validation errors
included — nothing throws synchronously). `join()` wraps them in a `CompletionException`;
`cancel(true)` aborts the request.

---

## Live console and server log

`console().connect(listener)` opens `/ws/console` (permission `console.write`); every console line
arrives in `onLine`, and `sendCommand` runs a command and completes with the server's
acknowledgement.

```java
try (LiveConsole console = client.console().connect(new ConsoleListener() {
        @Override public void onInit(List<String> lines) { /* scrollback */ }
        @Override public void onLine(String line) { System.out.println(line); }
        @Override public void onReconnecting(int attempt, Duration delay) { log.warn("reconnecting"); }
        @Override public void onClose(int code, String reason) { log.info("closed {} {}", code, reason); }
    })) {

    console.sendCommand("say Hello")
           .thenAccept(ack -> log.info("ok: {}", ack))
           .exceptionally(e -> { log.warn("rejected: {}", e.getMessage()); return null; });

    console.awaitClose();                 // block until the stream ends for good
}
```

- **Reconnects automatically** after network drops and closes `1001`, `1006`, `1011`, `1012`
  (backoff 1 s → 30 s, configurable with `LiveOptions`). It stops on `1008` (key revoked,
  permission or server access lost — reconnecting would fail the same way), `1009`, and when you
  call `close()`.
- **Commands are acknowledged in order:** each `sendCommand` future completes with the `ack`
  message or fails with `CommandException` (rejected, timed out — default 10 s, or the connection
  dropped before the answer came). Commands are limited to 1024 characters; the server allows
  10 commands per 10 seconds per connection.
- Callbacks run on the WebSocket's thread — keep them short.

```java
LiveConsole console = client.console().connect(listener, LiveOptions.defaults()
        .serverId(7)
        .commandTimeout(Duration.ofSeconds(5))
        .maxReconnectAttempts(20));
```

The server log works the same way and delivers structured entries:

```java
LiveServerLog log = client.serverLog().connect(new ServerLogListener() {
    @Override public void onEntry(JsonNode entry) { System.out.println(entry); }
});
```

Asynchronously: `client.async().console().connect(listener)` returns a
`CompletableFuture<LiveConsole>`. Closing the `TxApi` client closes all of its live streams.

---

## Webhooks

txApi can push events (bans, warnings, server offline/online, quota warnings, …) to your HTTPS
endpoint. Each delivery is signed with your endpoint's secret (`whsec_…`):
`X-Txapi-Signature: t=<unix seconds>,v1=<HMAC-SHA256(secret, "<t>.<raw body>")>`.

```java
WebhookEvent event = Webhooks.verify(rawBody, signatureHeader, secret);
switch (event.event()) {
    case WebhookEvents.PLAYER_BANNED -> handleBan(event.data());
    case WebhookEvents.SERVER_OFFLINE -> alert("Server " + event.serverId() + " is down");
    default -> { }
}
```

> **⚠ Verify the RAW request body — the exact bytes you received.**
> **If your framework parses the JSON and you serialise it again (`@RequestBody Map`, a DTO, a
> Jackson round-trip), whitespace and key order change and the signature will never match.**
> Read the body as `byte[]` and pass those bytes to `Webhooks.verify`.

`verify` checks the signature (constant-time, several `v1` entries during secret rotation are
fine) and that the timestamp is within 5 minutes (`Webhooks.verify(body, header, secret, tolerance)`
to change it), then parses the body. On any problem it throws `WebhookVerificationException` —
answer with 400. Deliveries are retried and can arrive more than once: deduplicate on `event.id()`.

**Spring Boot**

```java
@RestController
class TxApiWebhookController {
    @Value("${txapi.webhook-secret}") String secret;

    @PostMapping("/txapi-webhook")
    ResponseEntity<Void> receive(@RequestBody byte[] body,          // raw bytes, not a Map/DTO!
                                 @RequestHeader(Webhooks.SIGNATURE_HEADER) String signature) {
        WebhookEvent event;
        try {
            event = Webhooks.verify(body, signature, secret);
        } catch (WebhookVerificationException e) {
            return ResponseEntity.badRequest().build();
        }
        if (event.is(WebhookEvents.PLAYER_BANNED)) {
            String reason = event.data().path("request").path("reason").asText();
            // …
        }
        return ResponseEntity.noContent().build();
    }
}
```

**Plain JDK `HttpServer`** (no framework):

```java
HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
server.createContext("/txapi-webhook", exchange -> {
    try (exchange) {
        byte[] body = exchange.getRequestBody().readAllBytes();
        String sig = exchange.getRequestHeaders().getFirst(Webhooks.SIGNATURE_HEADER);
        try {
            WebhookEvent event = Webhooks.verify(body, sig, secret);
            exchange.sendResponseHeaders(204, -1);
            handle(event);
        } catch (WebhookVerificationException e) {
            exchange.sendResponseHeaders(400, -1);
        }
    }
});
server.start();
```

`WebhookEvents` has a constant for every event name (`WebhookEvents.ALL` lists them), and
`Webhooks.sign(body, secret, timestamp)` produces a valid header for testing your receiver.

---

## Logging and hooks

The SDK does no logging of its own. Use the hooks to log or measure requests; they never see the
API key.

```java
TxApi client = TxApi.builder()
        .apiKey(key)
        .onRequest(e -> log.debug("→ {} {} (attempt {})", e.method(), e.uri(), e.attempt()))
        .onResponse(e -> metrics.timer("txapi", "status", String.valueOf(e.status())).record(e.elapsed()))
        .build();
```

`onRequest` fires once per attempt, so retries are visible.

---

## Thread safety and lifecycle

- `TxApi`, its views and `TxApiAsync` are **thread-safe**; create one client and share it.
- Views (`forServer`, `withOptions`, `async()`) share the connection pool; closing a view does
  nothing, closing the root client closes live streams and releases the HTTP client it created.
  A `HttpClient` you passed in yourself is left alone.
- The SDK uses `java.net.http` only and starts no threads of its own: asynchronous retry delays
  and reconnect waits use `CompletableFuture.delayedExecutor`.

---

## Examples

Runnable programs live in [`examples/`](examples/examples) and are compiled with the tests, so
they always match the API:

| Example | What it shows |
|---|---|
| `QuickstartBan` | account info, search a player, ban for a week, iterate bans, error handling |
| `LiveConsole` | live console with scrollback, a command with acknowledgement, reconnect callbacks |
| `WebhookServer` | webhook receiver on the JDK `HttpServer`: raw body, verification, deduplication |

Run them after `mvn test-compile`:

```bash
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -Dmdep.includeScope=runtime
CP="target/classes:target/test-classes:$(cat cp.txt)"     # use ; instead of : on Windows

TXAPI_KEY=txp_… TXAPI_SERVER_ID=12 java -cp "$CP" examples.QuickstartBan "John"
TXAPI_KEY=txp_… TXAPI_SERVER_ID=12 java -cp "$CP" examples.LiveConsole "say Hello"
TXAPI_WEBHOOK_SECRET=whsec_…              java -cp "$CP" examples.WebhookServer 8081
```

---

## Building from source

```bash
mvn test        # unit tests against a local mock server — no network, no API key
mvn install     # build target/txapi-sdk-1.0.0.jar and install it locally
```

The tests cover all 67 endpoints, request building, error mapping, retries, rate limits,
pagination, webhooks (including the shared signature test vectors in `../spec/`) and the live
WebSocket client. Requires JDK 17+.

## Versioning

`TxApi.VERSION` is the SDK version (`1.0.0`), `TxApi.API_VERSION` the txApi API version it was
generated against (`1.8.0`). See [CHANGELOG.md](CHANGELOG.md).

## License

MIT — see [LICENSE](LICENSE).
