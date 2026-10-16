# txApi SDKs

Official client libraries for the [txApi](https://txapi.cloud) REST and WebSocket API — manage
your FiveM / RedM servers' txAdmin from your own code: search, ban and warn players, run console
commands, stream the live console, read the event log, manage the whitelist and txAdmin admins,
and verify webhooks.

| Language | Package | Install | Docs |
|---|---|---|---|
| TypeScript / JavaScript (Node 18+, Deno, Bun, edge) | [`@txapi/sdk`](https://www.npmjs.com/package/@txapi/sdk) | `npm install @txapi/sdk` | [typescript/](typescript/) |
| Python 3.9+ (sync + async) | [`txapi-sdk`](https://pypi.org/project/txapi-sdk/) | `pip install "txapi-sdk[live]"` | [python/](python/) |
| Java 17+ | [`cloud.txapi:txapi-sdk`](https://central.sonatype.com/artifact/cloud.txapi/txapi-sdk) | Maven / Gradle | [java/](java/) |
| FiveM / RedM (Lua 5.4, server) | resource `txapi` | zip from [Releases](https://github.com/txadmin-api/txapi-sdks/releases) | [fivem/](fivem/) |

Full guide with examples for every language: **<https://txapi.cloud/docs/sdks>** ·
API reference: <https://txapi.cloud/docs> · OpenAPI: <https://txapi.cloud/openapi.json>

```ts
import { TxApi } from "@txapi/sdk";

const txapi = new TxApi({ apiKey: process.env.TXAPI_KEY, serverId: 1 });
const { players } = await txapi.players.search({ query: "john" });
await txapi.players.ban({ identifier: `license:${players[0].license}`, reason: "Cheating", durationHours: 168 });
```

## What all four have in common

Every SDK implements the same contract, **[SPEC.md](SPEC.md)**:

- **One method per endpoint** — all endpoints of the API, checked by a coverage test against
  [`spec/endpoints.json`](spec/endpoints.json).
- **Typed errors** — one error type per status; the API key never appears in messages or logs.
- **Safe retries** — `429` with `Retry-After` is retried, a `POST` is **never** repeated after a
  network error (a ban or a restart must not run twice).
- **Rate-limit information** on every response, **auto-pagination**, **webhook signature
  verification** with shared test vectors ([`spec/webhook-vectors.json`](spec/webhook-vectors.json)).
- **Live console and server log** over WebSocket with reconnect (not in FiveM — FXServer's Lua has
  no WebSocket client and no HMAC; its README says what to use instead).

> **Keep your API key on the server.** A key in browser JavaScript or in a FiveM client script is
> readable by everyone who opens the page or joins the server.

## Versions

All four SDKs are released together under one version. Each release on
[GitHub Releases](https://github.com/txadmin-api/txapi-sdks/releases) lists the API version it
targets and carries the build files (npm tarball, Python wheel + sdist, Java jar + pom, FiveM
zip) with a `SHA256SUMS` file. The same files are served from `https://txapi.cloud/sdks/` for
setups without access to the package registries. Changes per SDK: each `CHANGELOG.md`.

## Development

| SDK | Tests |
|---|---|
| TypeScript | `cd typescript && npm ci && npm run check` |
| Python | `cd python && pip install -e ".[dev]" && pytest && mypy --strict src && ruff check .` |
| Java | `cd java && mvn verify` |
| FiveM | `sh fivem/tests/run.sh` (Lua 5.4, Node for the generator check) |

`sh scripts/check-version.sh` checks that all SDKs carry the same version,
`sh scripts/pack.sh all <dir>` builds the release files.

This repository is a **published mirror**: the SDKs are developed together with the API, and
every release replaces the contents here. Issues and pull requests are welcome — see
[CONTRIBUTING.md](CONTRIBUTING.md). Security problems: [SECURITY.md](SECURITY.md).

## License

MIT — see [LICENSE](LICENSE).
