import type { ApiPromise, RequestOptions } from "../core/http";
import { Paginator, nextEventCursor } from "../core/pagination";
import type {
  ActionLog,
  Activity,
  EventPage,
  EventSummary,
  Leaderboard,
  PlayerEventStats,
  Playtime,
  ServerEvent,
} from "../types";
import { Resource } from "./base";

/**
 * Time value for `since`/`until`: ISO-8601, epoch seconds/milliseconds, or relative to now
 * (`"30m"`, `"24h"`, `"7d"`). A `Date` is sent as ISO-8601.
 */
export type TimeValue = string | number | Date;

/** Common list parameters. Lists are newest first, `limit` 1–500 (default 100). */
export interface EventListBase {
  since?: TimeValue;
  /** Exclusive. */
  until?: TimeValue;
  /** `nextCursor` of the previous page. */
  cursor?: string;
  limit?: number;
}

export interface EventListParams extends EventListBase {
  /** Comma-separated or array: join, leave, join_denied, death, chat, command, menu, admin, ban, warn, system, other. */
  type?: string | string[];
  /** Name (exact, case-insensitive) or identifier — as actor or target. */
  player?: string;
  /** Text search in the message. */
  search?: string;
}

export interface DeathParams extends EventListBase {
  player?: string;
  victim?: string;
  killer?: string;
  /** Cause/weapon, exact. */
  cause?: string;
  /** `true` = killed by a player, `false` = no killer. */
  pvp?: boolean;
}

export interface KillParams extends EventListBase {
  player?: string;
  killer?: string;
  victim?: string;
  cause?: string;
}

export interface ChatParams extends EventListBase {
  player?: string;
  search?: string;
}

export interface AdminActionParams extends EventListBase {
  admin?: string;
  /** ban, warn, kick, revoke, command, restart, whitelist, auth, other. */
  kind?: string;
  search?: string;
}

export interface ActionParams extends EventListBase {
  type?: "ban" | "warn";
  player?: string;
  admin?: string;
  revoked?: boolean;
}

export interface SessionParams extends EventListBase {
  player?: string;
}

function time(value: TimeValue | undefined): string | number | undefined {
  return value instanceof Date ? value.toISOString() : value;
}

function listQuery(params: Record<string, unknown>): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(params)) {
    if (key === "since" || key === "until") out[key] = time(value as TimeValue | undefined);
    else if (Array.isArray(value)) out[key] = value.join(",");
    else out[key] = value;
  }
  return out;
}

type EventListFn<P> = (params: P, options?: RequestOptions) => ApiPromise<EventPage>;

/**
 * Stored server events (event log): deaths, kills, joins/leaves, admin actions, bans/warns
 * from any source, commands and (optionally) chat. Enable the event log per server in
 * the dashboard.
 */
export class EventsResource extends Resource {
  /** All events, filterable. Permission: `logs.view`. `GET /api/v1/events` */
  list(params: EventListParams = {}, options?: RequestOptions): ApiPromise<EventPage> {
    return this._t.request("events.list", { query: listQuery({ ...params }) }, options);
  }

  /** Auto-paging {@link EventsResource.list}. Permission: `logs.view`. */
  listAll(params: EventListParams = {}, options?: RequestOptions): Paginator<ServerEvent, EventPage> {
    return this.#paginate(this.list.bind(this), params, options);
  }

  /** Recent deaths (cause and killer). Permission: `logs.view`. `GET /api/v1/events/deaths` */
  deaths(params: DeathParams = {}, options?: RequestOptions): ApiPromise<EventPage> {
    return this._t.request("events.deaths", { query: listQuery({ ...params }) }, options);
  }

  /** Auto-paging {@link EventsResource.deaths}. Permission: `logs.view`. */
  deathsAll(params: DeathParams = {}, options?: RequestOptions): Paginator<ServerEvent, EventPage> {
    return this.#paginate(this.deaths.bind(this), params, options);
  }

  /** Recent kills (deaths caused by players). Permission: `logs.view`. `GET /api/v1/events/kills` */
  kills(params: KillParams = {}, options?: RequestOptions): ApiPromise<EventPage> {
    return this._t.request("events.kills", { query: listQuery({ ...params }) }, options);
  }

  /** Auto-paging {@link EventsResource.kills}. Permission: `logs.view`. */
  killsAll(params: KillParams = {}, options?: RequestOptions): Paginator<ServerEvent, EventPage> {
    return this.#paginate(this.kills.bind(this), params, options);
  }

  /**
   * Leaderboard: kills, deaths, K/D (`since` default 7d, `sort` kills|deaths|kd, `limit` 1–100).
   *
   * Permission: `logs.view`. `GET /api/v1/events/leaderboard`
   */
  leaderboard(
    params: { since?: TimeValue; sort?: "kills" | "deaths" | "kd"; limit?: number } = {},
    options?: RequestOptions,
  ): ApiPromise<Leaderboard> {
    return this._t.request("events.leaderboard", { query: listQuery({ ...params }) }, options);
  }

  /**
   * Statistics of one player (name or identifier). Unknown player → `found: false`, not 404.
   *
   * Permission: `logs.view`. `GET /api/v1/events/player`
   */
  player(player: string, params: { since?: TimeValue } = {}, options?: RequestOptions): ApiPromise<PlayerEventStats> {
    return this._t.request("events.player", { query: listQuery({ player, ...params }) }, options);
  }

  /** Summary: events per type, kills, players (`since` default 24h). Permission: `logs.view`. `GET /api/v1/events/summary` */
  summary(params: { since?: TimeValue } = {}, options?: RequestOptions): ApiPromise<EventSummary> {
    return this._t.request("events.summary", { query: listQuery({ ...params }) }, options);
  }

  /**
   * Chat history — only stored when chat logging is on (see `chatLog` in the response).
   *
   * Permission: `logs.view`. `GET /api/v1/events/chat`
   */
  chat(params: ChatParams = {}, options?: RequestOptions): ApiPromise<EventPage> {
    return this._t.request("events.chat", { query: listQuery({ ...params }) }, options);
  }

  /** Auto-paging {@link EventsResource.chat}. Permission: `logs.view`. */
  chatAll(params: ChatParams = {}, options?: RequestOptions): Paginator<ServerEvent, EventPage> {
    return this.#paginate(this.chat.bind(this), params, options);
  }

  /** Commands run by players. Permission: `logs.view`. `GET /api/v1/events/commands` */
  commands(params: ChatParams = {}, options?: RequestOptions): ApiPromise<EventPage> {
    return this._t.request("events.commands", { query: listQuery({ ...params }) }, options);
  }

  /** Auto-paging {@link EventsResource.commands}. Permission: `logs.view`. */
  commandsAll(params: ChatParams = {}, options?: RequestOptions): Paginator<ServerEvent, EventPage> {
    return this.#paginate(this.commands.bind(this), params, options);
  }

  /**
   * Admin actions from the txAdmin panel and in-game menu.
   *
   * Permission: `logs.view`. `GET /api/v1/events/admin-actions`
   */
  adminActions(params: AdminActionParams = {}, options?: RequestOptions): ApiPromise<EventPage> {
    return this._t.request("events.admin-actions", { query: listQuery({ ...params }) }, options);
  }

  /** Auto-paging {@link EventsResource.adminActions}. Permission: `logs.view`. */
  adminActionsAll(params: AdminActionParams = {}, options?: RequestOptions): Paginator<ServerEvent, EventPage> {
    return this.#paginate(this.adminActions.bind(this), params, options);
  }

  /** Bans and warns from any source. Permission: `logs.view`. `GET /api/v1/events/actions` */
  actions(params: ActionParams = {}, options?: RequestOptions): ApiPromise<EventPage> {
    return this._t.request("events.actions", { query: listQuery({ ...params }) }, options);
  }

  /** Auto-paging {@link EventsResource.actions}. Permission: `logs.view`. */
  actionsAll(params: ActionParams = {}, options?: RequestOptions): Paginator<ServerEvent, EventPage> {
    return this.#paginate(this.actions.bind(this), params, options);
  }

  /** Joins and leaves (with reason). Permission: `logs.view`. `GET /api/v1/events/sessions` */
  sessions(params: SessionParams = {}, options?: RequestOptions): ApiPromise<EventPage> {
    return this._t.request("events.sessions", { query: listQuery({ ...params }) }, options);
  }

  /** Auto-paging {@link EventsResource.sessions}. Permission: `logs.view`. */
  sessionsAll(params: SessionParams = {}, options?: RequestOptions): Paginator<ServerEvent, EventPage> {
    return this.#paginate(this.sessions.bind(this), params, options);
  }

  /**
   * Play time per player (from join/leave pairs; `since` default 7d, `limit` 1–100).
   *
   * Permission: `logs.view`. `GET /api/v1/events/playtime`
   */
  playtime(
    params: { since?: TimeValue; player?: string; limit?: number } = {},
    options?: RequestOptions,
  ): ApiPromise<Playtime> {
    return this._t.request("events.playtime", { query: listQuery({ ...params }) }, options);
  }

  /**
   * Activity per hour or day (`bucket=hour` max. 31 days back, `day` 366).
   *
   * Permission: `logs.view`. `GET /api/v1/events/activity`
   */
  activity(
    params: { since?: TimeValue; bucket?: "hour" | "day"; timeZone?: string } = {},
    options?: RequestOptions,
  ): ApiPromise<Activity> {
    return this._t.request("events.activity", { query: listQuery({ ...params }) }, options);
  }

  /**
   * txAdmin's admin log, live (not stored), newest last (`lines` 1–2000, default 200).
   *
   * Permission: `logs.view`. `GET /api/v1/txadmin/action-log`
   */
  actionLog(params: { lines?: number } = {}, options?: RequestOptions): ApiPromise<ActionLog> {
    return this._t.request("events.action-log", { query: { lines: params.lines } }, options);
  }

  #paginate<P extends EventListBase>(
    fn: EventListFn<P>,
    params: P,
    options: RequestOptions | undefined,
  ): Paginator<ServerEvent, EventPage> {
    return new Paginator<ServerEvent, EventPage>(
      (c) => fn({ ...params, ...c }, options),
      (p) => p.events ?? [],
      (p) => nextEventCursor(p),
      options?.signal,
    );
  }
}
