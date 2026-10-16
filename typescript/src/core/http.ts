import { ENDPOINTS, type EndpointDef, type EndpointId } from "../endpoints";
import {
  ConnectionError,
  TimeoutError,
  TxApiError,
  ValidationError,
  errorFromResponse,
} from "./errors";
import { parseRateLimit, parseRetryAfter, type RateLimit } from "./ratelimit";
import type { WebSocketConstructor } from "../ws/stream";

/** A `fetch`-compatible function (the global one, `undici`, a test double …). */
export type FetchLike = (input: string, init: RequestInit) => Promise<Response>;

/** Per-call options accepted by every endpoint method as the last argument. */
export interface RequestOptions {
  /**
   * txAdmin server to address (sent as `?serverId=`). Overrides the client default.
   * Ignored by account-level endpoints (`account.me`, `account.servers`).
   */
  serverId?: number | string | undefined;
  /** Cancels the request (including any wait between retries). */
  signal?: AbortSignal | undefined;
  /** Timeout per attempt in milliseconds. Overrides the client default. */
  timeout?: number | undefined;
  /** Retries for this call. Overrides the client default. */
  maxRetries?: number | undefined;
}

/** Metadata of an HTTP response. */
export interface ResponseMeta {
  status: number;
  statusText: string;
  headers: Headers;
  /** Parsed `X-RateLimit-*` headers, `null` when the response carried none. */
  rateLimit: RateLimit | null;
}

/** Parsed data together with the raw response metadata — see {@link ApiPromise.withResponse}. */
export interface ApiResponse<T> {
  data: T;
  response: ResponseMeta;
}

/** Passed to the `onRequest` hook before every attempt. Never contains the API key. */
export interface RequestHookInfo {
  /** Endpoint id from the catalog, e.g. `players.ban`. */
  endpoint: string;
  method: string;
  /** Full request URL (the key is sent in a header, never in the URL). */
  url: string;
  /** 0 for the first attempt, 1 for the first retry … */
  attempt: number;
}

/** Passed to the `onResponse` hook after every attempt. */
export interface ResponseHookInfo extends RequestHookInfo {
  /** HTTP status, `undefined` when the attempt failed without a response. */
  status: number | undefined;
  durationMs: number;
  rateLimit: RateLimit | null;
  /** Set when the attempt ended in an error (HTTP or transport). */
  error: TxApiError | undefined;
}

/** @internal */
export interface TransportConfig {
  apiKey: string;
  baseUrl: string;
  serverId: number | string | undefined;
  timeout: number;
  maxRetries: number;
  userAgent: string;
  fetch: FetchLike;
  onRequest: ((info: RequestHookInfo) => unknown) | undefined;
  onResponse: ((info: ResponseHookInfo) => unknown) | undefined;
  webSocket: WebSocketConstructor | undefined;
}

/** @internal Arguments of a single endpoint call. */
export interface CallArgs {
  path?: Record<string, string | number>;
  query?: Record<string, unknown>;
  body?: Record<string, unknown>;
}

/**
 * The promise every endpoint method returns. `await` it for the parsed data, or call
 * {@link ApiPromise.withResponse} to also get status, headers and rate limit.
 */
export class ApiPromise<T> implements Promise<T> {
  readonly [Symbol.toStringTag] = "ApiPromise";
  readonly #inner: Promise<ApiResponse<T>>;
  #data: Promise<T> | undefined;

  /** @internal */
  constructor(inner: Promise<ApiResponse<T>>) {
    this.#inner = inner;
  }

  /** Resolves with `{ data, response: { status, statusText, headers, rateLimit } }`. */
  withResponse(): Promise<ApiResponse<T>> {
    return this.#inner;
  }

  #parsed(): Promise<T> {
    if (!this.#data) this.#data = this.#inner.then((r) => r.data);
    return this.#data;
  }

  then<R1 = T, R2 = never>(
    onfulfilled?: ((value: T) => R1 | PromiseLike<R1>) | null,
    onrejected?: ((reason: unknown) => R2 | PromiseLike<R2>) | null,
  ): Promise<R1 | R2> {
    return this.#parsed().then(onfulfilled, onrejected);
  }

  catch<R = never>(onrejected?: ((reason: unknown) => R | PromiseLike<R>) | null): Promise<T | R> {
    return this.#parsed().catch(onrejected);
  }

  finally(onfinally?: (() => void) | null): Promise<T> {
    return this.#parsed().finally(onfinally);
  }
}

const RETRYABLE_STATUS = new Set([502, 503, 504]);
const MAX_RETRY_AFTER_SECONDS = 60;

/** Backoff for attempt n: full jitter over `min(8 s, 0.5 s · 2^n)`. @internal */
export function backoffMs(attempt: number, random: () => number = Math.random): number {
  const cap = Math.min(8000, 500 * 2 ** attempt);
  return Math.floor(random() * cap);
}

function abortReason(signal: AbortSignal): unknown {
  return signal.reason ?? new DOMException("The operation was aborted.", "AbortError");
}

/** Sleeps `ms`, rejecting immediately when `signal` aborts. @internal */
export function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(abortReason(signal));
      return;
    }
    const onAbort = () => {
      clearTimeout(timer);
      reject(abortReason(signal as AbortSignal));
    };
    const timer = setTimeout(() => {
      signal?.removeEventListener("abort", onAbort);
      resolve();
    }, ms);
    signal?.addEventListener("abort", onAbort, { once: true });
  });
}

function callHook(hook: ((info: never) => unknown) | undefined, info: unknown): void {
  if (!hook) return;
  try {
    const result = (hook as (i: unknown) => unknown)(info);
    if (result && typeof (result as PromiseLike<unknown>).then === "function") {
      (result as Promise<unknown>).then(undefined, () => undefined);
    }
  } catch {
    // Hooks are for logging/metrics and must never break a request.
  }
}

function isBlank(value: unknown): boolean {
  if (value === undefined || value === null) return true;
  if (typeof value === "string") return value.trim() === "";
  if (Array.isArray(value)) return value.length === 0;
  return false;
}

/** Client-side validation (SPEC §2): required fields present and non-blank, `maxLength`. @internal */
export function validateCall(id: string, def: EndpointDef, args: CallArgs): void {
  for (const name of def.requiredParams) {
    const value = args.path?.[name] ?? args.query?.[name];
    if (isBlank(value)) throw new ValidationError(`${id}: \`${name}\` is required`, name);
  }
  for (const field of def.body) {
    const value = args.body?.[field.name];
    if (field.required) {
      if (value === undefined || value === null) {
        throw new ValidationError(`${id}: \`${field.name}\` is required`, field.name);
      }
      if (isBlank(value)) {
        throw new ValidationError(`${id}: \`${field.name}\` must not be blank`, field.name);
      }
    }
    if (field.maxLength !== undefined && typeof value === "string" && value.length > field.maxLength) {
      throw new ValidationError(
        `${id}: \`${field.name}\` must be at most ${field.maxLength} characters (got ${value.length})`,
        field.name,
      );
    }
  }
}

/** Builds the absolute request URL. @internal */
export function buildUrl(
  baseUrl: string,
  def: EndpointDef,
  args: CallArgs,
  serverId: number | string | undefined,
): string {
  const path = def.path.replace(/\{(\w+)\}/g, (_, name: string) => {
    const value = args.path?.[name];
    if (value === undefined || value === null) throw new ValidationError(`\`${name}\` is required`, name);
    return encodeURIComponent(String(value));
  });
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(args.query ?? {})) {
    if (value === undefined || value === null) continue;
    search.append(key, String(value));
  }
  if (!def.accountLevel && serverId !== undefined && serverId !== null && String(serverId) !== "") {
    search.set("serverId", String(serverId));
  }
  const qs = search.toString();
  return `${baseUrl}${path}${qs ? `?${qs}` : ""}`;
}

/** JSON body without `undefined`/`null` fields (SPEC §2). @internal */
export function buildBody(body: Record<string, unknown> | undefined): string {
  const clean: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(body ?? {})) {
    if (value === undefined || value === null) continue;
    clean[key] = value;
  }
  return JSON.stringify(clean);
}

function parseBody(text: string, contentType: string | null): unknown {
  if (text === "") return undefined;
  const looksJson = /^\s*[[{"]/.test(text) || /^\s*(true|false|null|-?\d)/.test(text);
  if ((contentType ?? "").includes("json") || looksJson) {
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }
  return text;
}

/** @internal The request engine shared by all resources. Holds the API key privately. */
export class Transport {
  readonly #config: TransportConfig;
  /** Rate limit of the most recent response. */
  lastRateLimit: RateLimit | null = null;

  constructor(config: TransportConfig) {
    this.#config = config;
  }

  get baseUrl(): string {
    return this.#config.baseUrl;
  }

  get defaultServerId(): number | string | undefined {
    return this.#config.serverId;
  }

  /** @internal Only used to build the WebSocket URL (query auth). */
  get apiKeyForSocket(): string {
    return this.#config.apiKey;
  }

  /** @internal */
  get webSocket(): WebSocketConstructor | undefined {
    return this.#config.webSocket;
  }

  request<T>(id: EndpointId, args: CallArgs = {}, options: RequestOptions = {}): ApiPromise<T> {
    let inner: Promise<ApiResponse<T>>;
    try {
      const def: EndpointDef = ENDPOINTS[id];
      validateCall(id, def, args);
      const serverId = options.serverId ?? this.#config.serverId;
      const url = buildUrl(this.#config.baseUrl, def, args, serverId);
      const body = def.body.length > 0 ? buildBody(args.body) : undefined;
      inner = this.#execute<T>(id, def, url, body, options);
    } catch (err) {
      inner = Promise.reject(err);
    }
    return new ApiPromise<T>(inner);
  }

  async #execute<T>(
    id: string,
    def: EndpointDef,
    url: string,
    body: string | undefined,
    options: RequestOptions,
  ): Promise<ApiResponse<T>> {
    const cfg = this.#config;
    const maxRetries = Math.max(0, options.maxRetries ?? cfg.maxRetries);
    const timeout = options.timeout ?? cfg.timeout;
    const signal = options.signal;
    const isGet = def.method === "GET";

    for (let attempt = 0; ; attempt++) {
      if (signal?.aborted) throw abortReason(signal);
      const info: RequestHookInfo = { endpoint: id, method: def.method, url, attempt };
      callHook(cfg.onRequest, info);
      const started = Date.now();

      const controller = new AbortController();
      let timedOut = false;
      const timer = setTimeout(() => {
        timedOut = true;
        controller.abort();
      }, timeout);
      const onUserAbort = () => controller.abort();
      signal?.addEventListener("abort", onUserAbort, { once: true });

      const headers: Record<string, string> = {
        Authorization: `Bearer ${cfg.apiKey}`,
        Accept: "application/json",
        "User-Agent": cfg.userAgent,
      };
      if (body !== undefined) headers["Content-Type"] = "application/json";

      let response: Response;
      let text: string;
      try {
        response = await cfg.fetch(url, {
          method: def.method,
          headers,
          body,
          signal: controller.signal,
        });
        text = await response.text();
      } catch (cause) {
        clearTimeout(timer);
        signal?.removeEventListener("abort", onUserAbort);
        if (signal?.aborted) throw abortReason(signal);
        const error = timedOut
          ? new TimeoutError(`${def.method} ${def.path} timed out after ${timeout} ms`, { cause })
          : new ConnectionError(
              `${def.method} ${def.path} failed: ${connectionMessage(cause)}`,
              { cause },
            );
        callHook(cfg.onResponse, {
          ...info,
          status: undefined,
          durationMs: Date.now() - started,
          rateLimit: null,
          error,
        } satisfies ResponseHookInfo);
        if (isGet && attempt < maxRetries) {
          await sleep(backoffMs(attempt), signal);
          continue;
        }
        throw error;
      }
      clearTimeout(timer);
      signal?.removeEventListener("abort", onUserAbort);

      const rateLimit = parseRateLimit(response.headers);
      if (rateLimit) this.lastRateLimit = rateLimit;
      const parsed = parseBody(text, response.headers.get("content-type"));
      const meta: ResponseMeta = {
        status: response.status,
        statusText: response.statusText,
        headers: response.headers,
        rateLimit,
      };

      if (response.ok) {
        callHook(cfg.onResponse, {
          ...info,
          status: response.status,
          durationMs: Date.now() - started,
          rateLimit,
          error: undefined,
        } satisfies ResponseHookInfo);
        return { data: parsed as T, response: meta };
      }

      const retryAfter = parseRetryAfter(response.headers);
      const error = errorFromResponse(
        response.status,
        response.statusText,
        parsed,
        response.headers,
        rateLimit,
        retryAfter,
      );
      callHook(cfg.onResponse, {
        ...info,
        status: response.status,
        durationMs: Date.now() - started,
        rateLimit,
        error,
      } satisfies ResponseHookInfo);

      if (attempt < maxRetries) {
        if (response.status === 429) {
          if (retryAfter !== null && retryAfter <= MAX_RETRY_AFTER_SECONDS) {
            await sleep(retryAfter * 1000, signal);
            continue;
          }
        } else if (isGet && RETRYABLE_STATUS.has(response.status)) {
          const delay =
            retryAfter !== null && retryAfter <= MAX_RETRY_AFTER_SECONDS
              ? retryAfter * 1000
              : backoffMs(attempt);
          await sleep(delay, signal);
          continue;
        }
      }
      throw error;
    }
  }
}

function connectionMessage(cause: unknown): string {
  if (cause instanceof Error) {
    const inner = (cause as Error & { cause?: unknown }).cause;
    if (inner instanceof Error && inner.message) return `${cause.message} (${inner.message})`;
    return cause.message;
  }
  return String(cause);
}
