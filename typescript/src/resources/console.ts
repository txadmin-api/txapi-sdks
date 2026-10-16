import type { ApiPromise, RequestOptions } from "../core/http";
import type { ConsoleBuffer, MutationResult, ServerLogBuffer, ServerLogHistory } from "../types";
import {
  ConsoleStream,
  ServerLogStream,
  streamTarget,
  type ConsoleStreamOptions,
  type StreamOptions,
} from "../ws/stream";
import { Resource } from "./base";

/** Live console: buffer, commands and the real-time stream. */
export class ConsoleResource extends Resource {
  /**
   * The last 500 console lines (with FXServer's ANSI colour codes).
   *
   * Permission: `console.write`. `GET /api/v1/console`
   */
  buffer(options?: RequestOptions): ApiPromise<ConsoleBuffer> {
    return this._t.request("console.buffer", {}, options);
  }

  /**
   * Run a console command (max. 1024 characters). Fire-and-forget: the answer only means
   * txAdmin accepted it; read the output from {@link ConsoleResource.buffer} or the stream.
   *
   * Permission: `console.write`. `POST /api/v1/console/command`
   */
  command(command: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("console.command", { body: { command } }, options);
  }

  /**
   * Opens the live console WebSocket (`/ws/console`). Reconnects on 1001/1006/1011/1012
   * and network loss; stops for good on 1008/1009 (see {@link ConsoleStream.ready}).
   *
   * Permission: `console.write`.
   */
  connect(options: ConsoleStreamOptions = {}): ConsoleStream {
    const target = streamTarget(
      this._t.baseUrl,
      "/ws/console",
      this._t.apiKeyForSocket,
      options.serverId ?? this._t.defaultServerId,
      options,
      this._t.webSocket,
    );
    return new ConsoleStream(target, options.commandTimeout);
  }
}

/** Server log (joins, leaves, deaths, chat …): buffer, older pages and the live stream. */
export class ServerLogResource extends Resource {
  /**
   * The last 500 server-log entries (objects when structured, otherwise strings).
   *
   * Permission: `logs.view`. `GET /api/v1/server/log`
   */
  buffer(options?: RequestOptions): ApiPromise<ServerLogBuffer> {
    return this._t.request("serverlog.buffer", {}, options);
  }

  /**
   * Up to 500 entries before (`older`, default) or after (`newer`) `ref`
   * (epoch milliseconds, default now) — live from txAdmin.
   *
   * Permission: `logs.view`. `GET /api/v1/server/log/history`
   */
  history(
    params: { direction?: "older" | "newer"; ref?: number } = {},
    options?: RequestOptions,
  ): ApiPromise<ServerLogHistory> {
    return this._t.request(
      "serverlog.history",
      { query: { direction: params.direction, ref: params.ref } },
      options,
    );
  }

  /**
   * Opens the live server-log WebSocket (`/ws/serverlog`). Read-only.
   *
   * Permission: `logs.view`.
   */
  connect(options: StreamOptions = {}): ServerLogStream {
    const target = streamTarget(
      this._t.baseUrl,
      "/ws/serverlog",
      this._t.apiKeyForSocket,
      options.serverId ?? this._t.defaultServerId,
      options,
      this._t.webSocket,
    );
    return new ServerLogStream(target);
  }
}
