import type { ApiPromise, RequestOptions, Transport } from "../core/http";
import type { JsonObject, MutationResult, ServerStatus } from "../types";
import { Resource } from "./base";

/** Single resources of the FXServer. Names: letters, digits, `_ - .` only (max. 128). */
export class ServerResourcesResource extends Resource {
  /** Start a resource. Permission: `server.control`. `POST /api/v1/server/resource/start` */
  start(name: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.resource.start", { body: { name } }, options);
  }

  /** Stop a resource. Permission: `server.control`. `POST /api/v1/server/resource/stop` */
  stop(name: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.resource.stop", { body: { name } }, options);
  }

  /** Restart a resource. Permission: `server.control`. `POST /api/v1/server/resource/restart` */
  restart(name: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.resource.restart", { body: { name } }, options);
  }

  /** Start a resource if it is stopped. Permission: `server.control`. `POST /api/v1/server/resource/ensure` */
  ensure(name: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.resource.ensure", { body: { name } }, options);
  }

  /** Rescan the resources folder. Permission: `server.control`. `POST /api/v1/server/resource/refresh` */
  refresh(options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.resource.refresh", {}, options);
  }
}

/** Server control: status, restart, resources, announcements, kick-all. */
export class ServerResource extends Resource {
  /** Single-resource operations. */
  readonly resources: ServerResourcesResource;

  /** @internal */
  constructor(transport: Transport) {
    super(transport);
    this.resources = new ServerResourcesResource(transport);
  }

  /**
   * Online state, txAdmin & FXServer version, player database stats.
   * `online` = the FXServer process runs; `txAdminReachable` = the panel answers.
   *
   * Permission: `server.control`. `GET /api/v1/server/status`
   */
  status(options?: RequestOptions): ApiPromise<ServerStatus> {
    return this._t.request("server.status", {}, options);
  }

  /** txAdmin diagnostics (passthrough). Permission: `server.control`. `GET /api/v1/server/diagnostics` */
  diagnostics(options?: RequestOptions): ApiPromise<JsonObject> {
    return this._t.request("server.diagnostics", {}, options);
  }

  /** Player-drop statistics (passthrough). Permission: `server.control`. `GET /api/v1/server/drops` */
  drops(options?: RequestOptions): ApiPromise<JsonObject> {
    return this._t.request("server.drops", {}, options);
  }

  /**
   * Tick-time histogram of an FXServer thread (passthrough, live from txAdmin).
   *
   * Permission: `server.control`. `GET /api/v1/server/performance`
   */
  performance(
    params: { thread?: "svMain" | "svNetwork" | "svSync" | (string & {}) } = {},
    options?: RequestOptions,
  ): ApiPromise<JsonObject> {
    return this._t.request("server.performance", { query: { thread: params.thread } }, options);
  }

  /** Restart the FXServer. Permission: `server.control`. `POST /api/v1/server/restart` */
  restart(options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.restart", {}, options);
  }

  /** Start the FXServer. Permission: `server.control`. `POST /api/v1/server/start` */
  start(options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.start", {}, options);
  }

  /** Stop the FXServer. Permission: `server.control`. `POST /api/v1/server/stop` */
  stop(options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.stop", {}, options);
  }

  /**
   * One-off restart through txAdmin's scheduler: `"HH:MM"` (24 h, txAdmin host time)
   * or `"+N"` minutes from now (1–1439).
   *
   * Permission: `server.control`. `POST /api/v1/server/schedule-restart`
   */
  scheduleRestart(time: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.schedule-restart", { body: { time } }, options);
  }

  /**
   * Skip (`true`) or un-skip (`false`) the next scheduled restart.
   *
   * Permission: `server.control`. `POST /api/v1/server/skip-restart`
   */
  skipRestart(skip: boolean, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.skip-restart", { body: { skip } }, options);
  }

  /** Kick every player (e.g. for maintenance). Permission: `server.control`. `POST /api/v1/server/kickall` */
  kickAll(reason?: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.kickall", { body: { reason } }, options);
  }

  /** Broadcast an announcement. Permission: `server.control`. `POST /api/v1/server/announcement` */
  announce(message: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("server.announcement", { body: { message } }, options);
  }
}
