import { ValidationError } from "./core/errors";
import type { FetchLike, RequestHookInfo, ResponseHookInfo } from "./core/http";
import { Transport } from "./core/http";
import type { RateLimit } from "./core/ratelimit";
import { AccountResource } from "./resources/account";
import { AdminsResource } from "./resources/admins";
import { ConsoleResource, ServerLogResource } from "./resources/console";
import { EventsResource } from "./resources/events";
import { HistoryResource } from "./resources/history";
import { PlayersResource } from "./resources/players";
import { ServerResource } from "./resources/server";
import { WhitelistResource } from "./resources/whitelist";
import { VERSION } from "./version";
import type { WebSocketConstructor } from "./ws/stream";

/** Default API host. */
export const DEFAULT_BASE_URL = "https://txapi.cloud";

/** Options of {@link TxApi}. */
export interface TxApiOptions {
  /** API key (`txp_…`). Falls back to the `TXAPI_KEY` environment variable. Required. */
  apiKey?: string | undefined;
  /** Host, default `TXAPI_BASE_URL` or `https://txapi.cloud`. `/api/v1` is appended by the SDK. */
  baseUrl?: string | undefined;
  /** Default server for every server-scoped call (`?serverId=`); each call can override it. */
  serverId?: number | string | undefined;
  /** Timeout per attempt in milliseconds, default 30 000. */
  timeout?: number | undefined;
  /** Automatic retries, default 2 (`0` disables). See the README for what is retried. */
  maxRetries?: number | undefined;
  /** Appended to the `User-Agent` (`txapi-typescript/<version> <yours>`). Ignored by browsers. */
  userAgent?: string | undefined;
  /** Custom `fetch` (proxies, test doubles …). Default: `globalThis.fetch`. */
  fetch?: FetchLike | undefined;
  /** WebSocket implementation for live streams. Default: `globalThis.WebSocket` (Node ≥ 22, Deno, Bun, browsers). */
  WebSocket?: WebSocketConstructor | undefined;
  /** Called before every attempt (logging/metrics). Errors thrown here are ignored. */
  onRequest?: ((info: RequestHookInfo) => unknown) | undefined;
  /** Called after every attempt (logging/metrics). Errors thrown here are ignored. */
  onResponse?: ((info: ResponseHookInfo) => unknown) | undefined;
}

function env(name: string): string | undefined {
  const proc = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process;
  const value = proc?.env?.[name];
  return value === undefined || value === "" ? undefined : value;
}

function normaliseBaseUrl(raw: string): string {
  let url = raw.trim().replace(/\/+$/, "");
  // Tolerate a base URL that already ends in /api/v1 — the SDK appends it itself.
  url = url.replace(/\/api\/v1$/i, "");
  return url;
}

/**
 * txApi client.
 *
 * ```ts
 * import { TxApi } from "@txapi/sdk";
 *
 * const txapi = new TxApi({ apiKey: process.env.TXAPI_KEY, serverId: 1 });
 * const { players } = await txapi.players.online();
 * ```
 *
 * Use it on a server only — never ship the API key to a browser.
 */
export class TxApi {
  /** Account: plan, quota, key, servers. */
  readonly account: AccountResource;
  /** Players: search, lookup, ban, kick, warn, DM, notes, whitelist. */
  readonly players: PlayersResource;
  /** Ban/warn history and revoking. */
  readonly history: HistoryResource;
  /** Server control and resources. */
  readonly server: ServerResource;
  /** Live console: buffer, commands, WebSocket stream. */
  readonly console: ConsoleResource;
  /** Server log: buffer, history, WebSocket stream. */
  readonly serverlog: ServerLogResource;
  /** Stored event log: deaths, kills, sessions, admin actions … */
  readonly events: EventsResource;
  /** Whitelist requests and pre-approvals. */
  readonly whitelist: WhitelistResource;
  /** txAdmin panel admins. */
  readonly admins: AdminsResource;

  /** Host the client talks to (without `/api/v1`). */
  readonly baseUrl: string;
  /** Default server id, if any. */
  readonly serverId: number | string | undefined;

  readonly #transport: Transport;

  constructor(options: TxApiOptions = {}) {
    const apiKey = options.apiKey ?? env("TXAPI_KEY");
    if (!apiKey || apiKey.trim() === "") {
      throw new ValidationError(
        "Missing API key: pass `apiKey` or set the TXAPI_KEY environment variable",
        "apiKey",
      );
    }
    const fetchImpl = options.fetch ?? (globalThis.fetch ? globalThis.fetch.bind(globalThis) : undefined);
    if (!fetchImpl) {
      throw new ValidationError("No fetch implementation available; pass `fetch` in the options", "fetch");
    }
    this.baseUrl = normaliseBaseUrl(options.baseUrl ?? env("TXAPI_BASE_URL") ?? DEFAULT_BASE_URL);
    this.serverId = options.serverId;
    const userAgent = `txapi-typescript/${VERSION}${options.userAgent ? ` ${options.userAgent}` : ""}`;
    this.#transport = new Transport({
      apiKey: apiKey.trim(),
      baseUrl: this.baseUrl,
      serverId: options.serverId,
      timeout: options.timeout ?? 30_000,
      maxRetries: options.maxRetries ?? 2,
      userAgent,
      fetch: fetchImpl as FetchLike,
      onRequest: options.onRequest,
      onResponse: options.onResponse,
      webSocket: options.WebSocket,
    });
    const t = this.#transport;
    this.account = new AccountResource(t);
    this.players = new PlayersResource(t);
    this.history = new HistoryResource(t);
    this.server = new ServerResource(t);
    this.console = new ConsoleResource(t);
    this.serverlog = new ServerLogResource(t);
    this.events = new EventsResource(t);
    this.whitelist = new WhitelistResource(t);
    this.admins = new AdminsResource(t);
  }

  /** Rate limit reported by the most recent response (`null` before the first one). */
  get lastRateLimit(): RateLimit | null {
    return this.#transport.lastRateLimit;
  }

  toString(): string {
    return `TxApi { baseUrl: ${JSON.stringify(this.baseUrl)}, serverId: ${JSON.stringify(this.serverId ?? null)}, apiKey: "***" }`;
  }

  toJSON(): Record<string, unknown> {
    return { baseUrl: this.baseUrl, serverId: this.serverId ?? null, apiKey: "***" };
  }

  /** Node.js `util.inspect` / `console.log` — never prints the key. */
  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return this.toString();
  }
}
