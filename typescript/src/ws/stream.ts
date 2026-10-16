import {
  CommandRejectedError,
  ConnectionError,
  StreamClosedError,
  TimeoutError,
  TxApiError,
  ValidationError,
} from "../core/errors";
import type { ServerLogEntry } from "../types";
import { AsyncQueue, TypedEmitter } from "./emitter";

/** The subset of the WHATWG `WebSocket` the SDK needs (global `WebSocket`, the `ws` package …). */
export interface WebSocketLike {
  readonly readyState: number;
  send(data: string): void;
  close(code?: number, reason?: string): void;
  addEventListener(type: "open", listener: (event: unknown) => void): void;
  addEventListener(type: "message", listener: (event: { data: unknown }) => void): void;
  addEventListener(type: "close", listener: (event: { code: number; reason: string }) => void): void;
  addEventListener(type: "error", listener: (event: unknown) => void): void;
}

/** A WebSocket constructor, e.g. `globalThis.WebSocket` or `import { WebSocket } from "ws"`. */
export type WebSocketConstructor = new (url: string) => WebSocketLike;

/** Reconnect policy of a live stream. */
export interface ReconnectOptions {
  /** Default `true`. */
  enabled?: boolean;
  /** First delay, default 1000 ms. Doubles per attempt (with jitter). */
  initialDelayMs?: number;
  /** Upper bound, default 30 000 ms. */
  maxDelayMs?: number;
  /** Give up after this many consecutive attempts. Default: never. */
  maxAttempts?: number;
}

/** Options of `console.connect()` / `serverlog.connect()`. */
export interface StreamOptions {
  /** Server to stream. Defaults to the client's `serverId`. */
  serverId?: number | string;
  /** `false` disables reconnecting, or fine-tune the policy. */
  reconnect?: boolean | ReconnectOptions;
  /** WebSocket implementation; defaults to the client option, then `globalThis.WebSocket`. */
  WebSocket?: WebSocketConstructor;
}

/** Options of `console.connect()`. */
export interface ConsoleStreamOptions extends StreamOptions {
  /** How long `sendCommand` waits for the `ack`, default 10 000 ms. */
  commandTimeout?: number;
}

/** Close codes after which the stream reconnects on its own. */
export const RECONNECT_CLOSE_CODES: readonly number[] = [1001, 1006, 1011, 1012];

/** Passed to `close` listeners. */
export interface StreamCloseInfo {
  code: number;
  reason: string;
  /** `true` when a reconnect is scheduled; `false` when the stream is finished. */
  willReconnect: boolean;
}

/** Passed to `reconnecting` listeners. */
export interface StreamReconnectInfo {
  /** 1 for the first reconnect attempt after a drop. */
  attempt: number;
  delayMs: number;
  /** Close code that caused the reconnect. */
  code: number;
}

/** Events every live stream emits. */
export interface BaseStreamEvents {
  /** The socket is open (also after every reconnect). */
  open: [];
  /** An `error` frame that did not belong to a pending command (stream stays open). */
  error: [error: TxApiError];
  close: [info: StreamCloseInfo];
  reconnecting: [info: StreamReconnectInfo];
}

// ── Console ────────────────────────────────────────────────────────────────

/** Frames the server sends on `/ws/console`. */
export type ConsoleFrame =
  | { type: "init"; lines: string[] }
  | { type: "log"; lines: string[] }
  | { type: "ack"; message: string }
  | { type: "error"; message: string }
  | { type: string; [key: string]: unknown };

export interface ConsoleStreamEvents extends BaseStreamEvents {
  /** Full console buffer, sent on connect and again after every reconnect. */
  init: [lines: string[]];
  /** One call per new console line. */
  log: [line: string];
  /** An `ack` that did not belong to a pending `sendCommand`. */
  ack: [message: string];
  /** Every raw frame, before it is dispatched. */
  frame: [frame: ConsoleFrame];
}

/** Item of the console's async iterator: one per line, tagged with the frame it came from. */
export interface ConsoleStreamItem {
  type: "init" | "log";
  line: string;
}

// ── Server log ─────────────────────────────────────────────────────────────

/** Frames the server sends on `/ws/serverlog`. */
export type ServerLogFrame =
  | { type: "init"; entries: ServerLogEntry[] }
  | { type: "log"; entries: ServerLogEntry[] }
  | { type: "error"; message: string }
  | { type: string; [key: string]: unknown };

export interface ServerLogStreamEvents extends BaseStreamEvents {
  /** Full server-log buffer, sent on connect and again after every reconnect. */
  init: [entries: ServerLogEntry[]];
  /** One call per new entry (object when txAdmin structured it, otherwise string). */
  log: [entry: ServerLogEntry];
  /** Every raw frame, before it is dispatched. */
  frame: [frame: ServerLogFrame];
}

/** Item of the server log's async iterator. */
export interface ServerLogStreamItem {
  type: "init" | "log";
  entry: ServerLogEntry;
}

const OPEN = 1;

function decode(data: unknown): string | null {
  if (typeof data === "string") return data;
  if (data instanceof ArrayBuffer) return new TextDecoder().decode(data);
  if (ArrayBuffer.isView(data)) return new TextDecoder().decode(data);
  return null;
}

/** @internal Everything the SDK needs to open a socket. */
export interface StreamTarget {
  /** Full URL including the API key — never surfaced in errors or events. */
  url: string;
  WebSocket: WebSocketConstructor;
  reconnect: Required<ReconnectOptions>;
}

/**
 * Base class of {@link ConsoleStream} and {@link ServerLogStream}: connection lifecycle,
 * reconnect policy, async iteration.
 */
export abstract class LiveStream<
  Events extends BaseStreamEvents & { [K in keyof Events]: unknown[] },
  Item,
> extends TypedEmitter<Events> {
  readonly #target: StreamTarget;
  #ws: WebSocketLike | null = null;
  #attempt = 0;
  #openedAt = 0;
  #reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  #closedByUser = false;
  #finished = false;
  #everOpened = false;
  readonly #queues = new Set<AsyncQueue<Item>>();
  readonly #ready: Promise<void>;
  #resolveReady!: () => void;
  #rejectReady!: (e: unknown) => void;
  readonly #closed: Promise<StreamCloseInfo>;
  #resolveClosed!: (info: StreamCloseInfo) => void;

  /** @internal */
  constructor(target: StreamTarget) {
    super();
    this.#target = target;
    this.#ready = new Promise<void>((resolve, reject) => {
      this.#resolveReady = resolve;
      this.#rejectReady = reject;
    });
    this.#ready.catch(() => undefined);
    this.#closed = new Promise((resolve) => {
      this.#resolveClosed = resolve;
    });
    // Defer so listeners attached right after connect() see the first events.
    queueMicrotask(() => this.#open());
  }

  /** `true` while the socket is open. */
  get connected(): boolean {
    return this.#ws?.readyState === OPEN;
  }

  /** `true` once the stream ended for good (user `close()` or a non-reconnectable close). */
  get finished(): boolean {
    return this.#finished;
  }

  /**
   * Resolves once the server has accepted the stream for the first time — i.e. when the first
   * `init` frame arrived (the server authenticates after the WebSocket handshake, so a bare
   * `open` does not mean much). Rejects with a {@link StreamClosedError} (e.g. 1008 — bad
   * key/permission/quota) if the stream ends for good first.
   *
   * Attach `init` listeners or start iterating **before** awaiting this, otherwise the
   * first buffer has already been delivered.
   */
  ready(): Promise<void> {
    return this.#ready;
  }

  /** Resolves with the final close info once the stream has ended for good. */
  closed(): Promise<StreamCloseInfo> {
    return this.#closed;
  }

  /** Closes the stream; no reconnect afterwards. Resolves when it is finished. */
  close(): Promise<StreamCloseInfo> {
    if (this.#finished) return this.#closed;
    this.#closedByUser = true;
    if (this.#reconnectTimer) {
      clearTimeout(this.#reconnectTimer);
      this.#reconnectTimer = null;
    }
    const ws = this.#ws;
    if (ws && (ws.readyState === 0 || ws.readyState === OPEN)) {
      try {
        ws.close(1000, "client closed");
      } catch {
        this.#finish(1000, "client closed");
      }
    } else {
      this.#finish(1000, "client closed");
    }
    return this.#closed;
  }

  [Symbol.asyncIterator](): AsyncIterableIterator<Item> {
    const queue: AsyncQueue<Item> = new AsyncQueue<Item>(() => this.#queues.delete(queue));
    if (this.#finished) queue.end();
    else this.#queues.add(queue);
    return queue;
  }

  /** @internal Called with every parsed frame. */
  protected abstract handleFrame(frame: Record<string, unknown>): void;

  /** @internal Called whenever a connection closes (before a reconnect or the end). */
  protected abstract onDisconnect(): void;

  /** @internal Called by subclasses when the server confirmed the stream (`init` frame). */
  protected markReady(): void {
    if (this.#everOpened) return;
    this.#everOpened = true;
    this.#resolveReady();
  }

  /** @internal Feeds the async iterators. */
  protected pushItem(item: Item): void {
    for (const queue of this.#queues) queue.push(item);
  }

  /** @internal Emits one of the base events from generic code. */
  #emitBase<K extends keyof BaseStreamEvents>(event: K, ...args: BaseStreamEvents[K]): void {
    (this.emit as unknown as (e: K, ...a: BaseStreamEvents[K]) => void).call(this, event, ...args);
  }

  /** @internal */
  protected get socket(): WebSocketLike | null {
    return this.#ws;
  }

  #open(): void {
    if (this.#closedByUser || this.#finished) return;
    let ws: WebSocketLike;
    try {
      ws = new this.#target.WebSocket(this.#target.url);
    } catch (cause) {
      // Never include the URL — it carries the API key.
      this.#emitBase("error", new ConnectionError("Could not open the WebSocket", { cause: stripUrl(cause) }));
      this.#finish(1006, "could not open the WebSocket");
      return;
    }
    this.#ws = ws;
    ws.addEventListener("open", () => {
      if (this.#ws !== ws) return;
      this.#openedAt = Date.now();
      this.#emitBase("open");
    });
    ws.addEventListener("message", (event) => {
      if (this.#ws !== ws) return;
      const text = decode(event.data);
      if (text === null) return;
      let frame: unknown;
      try {
        frame = JSON.parse(text);
      } catch {
        return;
      }
      if (frame && typeof frame === "object" && !Array.isArray(frame)) {
        this.handleFrame(frame as Record<string, unknown>);
      }
    });
    ws.addEventListener("error", () => {
      // A `close` event (usually 1006) always follows; it decides what happens next.
    });
    ws.addEventListener("close", (event) => {
      if (this.#ws !== ws) return;
      this.#ws = null;
      this.#handleClose(typeof event.code === "number" ? event.code : 1006, String(event.reason ?? ""));
    });
  }

  #handleClose(code: number, reason: string): void {
    this.onDisconnect();
    const policy = this.#target.reconnect;
    const reconnectable = RECONNECT_CLOSE_CODES.includes(code);
    if (this.#closedByUser || !policy.enabled || !reconnectable) {
      this.#finish(code, reason);
      return;
    }
    // A connection that stayed up for a while resets the backoff.
    if (this.#openedAt && Date.now() - this.#openedAt >= 10_000) this.#attempt = 0;
    this.#openedAt = 0;
    this.#attempt++;
    if (this.#attempt > policy.maxAttempts) {
      this.#finish(code, reason);
      return;
    }
    const base = Math.min(policy.maxDelayMs, policy.initialDelayMs * 2 ** (this.#attempt - 1));
    const delayMs = Math.round(base / 2 + Math.random() * (base / 2));
    this.#emitBase("close", { code, reason, willReconnect: true });
    this.#emitBase("reconnecting", { attempt: this.#attempt, delayMs, code });
    this.#reconnectTimer = setTimeout(() => {
      this.#reconnectTimer = null;
      this.#open();
    }, delayMs);
  }

  #finish(code: number, reason: string): void {
    if (this.#finished) return;
    this.#finished = true;
    this.#ws = null;
    const info: StreamCloseInfo = { code, reason, willReconnect: false };
    const byUser = this.#closedByUser;
    const error = byUser || code === 1000 ? undefined : new StreamClosedError(code, reason);
    if (!this.#everOpened) this.#rejectReady(error ?? new StreamClosedError(code, reason));
    this.onDisconnect();
    for (const queue of this.#queues) queue.end(error);
    this.#queues.clear();
    this.#emitBase("close", info);
    this.#resolveClosed(info);
  }
}

function stripUrl(cause: unknown): unknown {
  if (cause instanceof Error) return new Error(cause.message.replace(/wss?:\/\/\S+/g, "<url>"));
  return undefined;
}

interface PendingCommand {
  settled: boolean;
  resolve: (message: string) => void;
  reject: (error: unknown) => void;
  timer: ReturnType<typeof setTimeout>;
}

/** Max. length of a console command (characters). */
export const MAX_COMMAND_LENGTH = 1024;

/**
 * Live console (`/ws/console`, permission `console.write`).
 *
 * ```ts
 * const live = client.console.connect();
 * live.on("log", (line) => console.log(line));
 * await live.ready();
 * await live.sendCommand("status");
 * ```
 */
export class ConsoleStream extends LiveStream<ConsoleStreamEvents, ConsoleStreamItem> {
  readonly #pending: PendingCommand[] = [];
  readonly #commandTimeout: number;

  /** @internal */
  constructor(target: StreamTarget, commandTimeout = 10_000) {
    super(target);
    this.#commandTimeout = commandTimeout;
  }

  /**
   * Sends a console command and resolves with the server's `ack` message
   * (txAdmin accepted it; the output arrives as `log` lines). Rejects with
   * {@link CommandRejectedError} on an `error` frame, {@link TimeoutError} after the
   * command timeout, {@link ConnectionError} when not connected or the connection drops.
   * Acks are matched to commands in order (FIFO).
   */
  sendCommand(command: string): Promise<string> {
    if (typeof command !== "string" || command.trim() === "") {
      return Promise.reject(new ValidationError("`command` must not be blank", "command"));
    }
    if (command.length > MAX_COMMAND_LENGTH) {
      return Promise.reject(
        new ValidationError(
          `\`command\` must be at most ${MAX_COMMAND_LENGTH} characters (got ${command.length})`,
          "command",
        ),
      );
    }
    const ws = this.socket;
    if (!ws || ws.readyState !== OPEN) {
      return Promise.reject(new ConnectionError("The live console is not connected"));
    }
    return new Promise<string>((resolve, reject) => {
      const entry: PendingCommand = {
        settled: false,
        resolve: (message) => {
          if (entry.settled) return;
          entry.settled = true;
          clearTimeout(entry.timer);
          resolve(message);
        },
        reject: (error) => {
          if (entry.settled) return;
          entry.settled = true;
          clearTimeout(entry.timer);
          reject(error);
        },
        timer: setTimeout(() => {
          // Stays in the queue so a late ack is consumed by this entry, not the next command.
          entry.reject(new TimeoutError(`No ack for the command within ${this.#commandTimeout} ms`));
        }, this.#commandTimeout),
      };
      this.#pending.push(entry);
      try {
        ws.send(JSON.stringify({ type: "command", command }));
      } catch (cause) {
        this.#pending.splice(this.#pending.indexOf(entry), 1);
        entry.reject(new ConnectionError("Could not send the command", { cause: stripUrl(cause) }));
      }
    });
  }

  protected override handleFrame(frame: Record<string, unknown>): void {
    this.emit("frame", frame as ConsoleFrame);
    const type = frame.type;
    if (type === "init" || type === "log") {
      const lines = Array.isArray(frame.lines) ? frame.lines.map((l) => String(l)) : [];
      if (type === "init") {
        this.markReady();
        this.emit("init", lines);
      }
      for (const line of lines) {
        if (type === "log") this.emit("log", line);
        this.pushItem({ type, line });
      }
    } else if (type === "ack") {
      const message = typeof frame.message === "string" ? frame.message : "";
      const pending = this.#pending.shift();
      if (pending) pending.resolve(message);
      else this.emit("ack", message);
    } else if (type === "error") {
      const message = typeof frame.message === "string" ? frame.message : "error";
      const pending = this.#pending.shift();
      if (pending) pending.reject(new CommandRejectedError(message));
      else this.emit("error", new TxApiError(message));
    }
  }

  protected override onDisconnect(): void {
    for (const pending of this.#pending.splice(0)) {
      pending.reject(
        new ConnectionError("The connection closed before the command was acknowledged (it may or may not have run)"),
      );
    }
  }
}

/**
 * Live server log (`/ws/serverlog`, permission `logs.view`). Read-only.
 *
 * ```ts
 * const log = client.serverlog.connect();
 * for await (const { type, entry } of log) console.log(type, entry);
 * ```
 */
export class ServerLogStream extends LiveStream<ServerLogStreamEvents, ServerLogStreamItem> {
  protected override handleFrame(frame: Record<string, unknown>): void {
    this.emit("frame", frame as ServerLogFrame);
    const type = frame.type;
    if (type === "init" || type === "log") {
      const entries = (Array.isArray(frame.entries) ? frame.entries : []) as ServerLogEntry[];
      if (type === "init") {
        this.markReady();
        this.emit("init", entries);
      }
      for (const entry of entries) {
        if (type === "log") this.emit("log", entry);
        this.pushItem({ type, entry });
      }
    } else if (type === "error") {
      const message = typeof frame.message === "string" ? frame.message : "error";
      this.emit("error", new TxApiError(message));
    }
  }

  protected override onDisconnect(): void {
    /* nothing pending on a read-only stream */
  }
}

/** @internal Builds the socket URL (query auth) and resolves the constructor/policy. */
export function streamTarget(
  baseUrl: string,
  path: string,
  apiKey: string,
  serverId: number | string | undefined,
  options: StreamOptions,
  clientWebSocket: WebSocketConstructor | undefined,
): StreamTarget {
  const WS =
    options.WebSocket ??
    clientWebSocket ??
    (globalThis as { WebSocket?: WebSocketConstructor }).WebSocket;
  if (!WS) {
    throw new TxApiError(
      "No WebSocket implementation found. On Node.js 18/20 install `ws` and pass it: " +
        "new TxApi({ WebSocket }) with `import { WebSocket } from \"ws\"`.",
    );
  }
  const wsBase = baseUrl.replace(/^http(s?):\/\//i, (_m, s: string) => `ws${s}://`);
  const params = new URLSearchParams({ apiKey });
  if (serverId !== undefined && serverId !== null && String(serverId) !== "") {
    params.set("serverId", String(serverId));
  }
  const r = options.reconnect;
  const policy: Required<ReconnectOptions> = {
    enabled: r !== false && !(typeof r === "object" && r.enabled === false),
    initialDelayMs: typeof r === "object" && r.initialDelayMs !== undefined ? r.initialDelayMs : 1000,
    maxDelayMs: typeof r === "object" && r.maxDelayMs !== undefined ? r.maxDelayMs : 30_000,
    maxAttempts: typeof r === "object" && r.maxAttempts !== undefined ? r.maxAttempts : Number.POSITIVE_INFINITY,
  };
  return { url: `${wsBase}${path}?${params.toString()}`, WebSocket: WS, reconnect: policy };
}
