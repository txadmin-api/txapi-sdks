import type { ApiPromise, RequestOptions } from "../core/http";
import { Paginator, nextPlayerCursor } from "../core/pagination";
import type { JsonObject, MutationResult, PlayerPage, PlayerRow } from "../types";
import { Resource } from "./base";

/** Sort key for player lists. `sessions`/`lastConnection` are legacy aliases. */
export type PlayerSortBy = "playTime" | "tsJoined" | "tsLastConnection" | "sessions" | "lastConnection";
/** txAdmin's quick filters. */
export type PlayerFilter = "isOnline" | "isAdmin" | "isWhitelisted" | "hasNote";
export type SortOrder = "asc" | "desc";

/** Cursor parameters of a player list: `offset` = sort-key value, `cursor` = `license` of the last row. */
export interface PlayerCursorParams {
  offset?: number;
  cursor?: string;
}

export interface PlayerSearchParams extends PlayerCursorParams {
  /** Substring of the display name; empty = all players. */
  query?: string;
  /** Default `playTime`. */
  sortBy?: PlayerSortBy;
  /** Default `desc`. */
  order?: SortOrder;
  /** Quick filters, as an array or comma-separated string. */
  filters?: PlayerFilter[] | string;
}

export interface WarnParams {
  identifier: string;
  /** Shown to the player, at least 3 characters, max. 512. */
  reason: string;
}

export interface BanParams {
  identifier: string;
  /** At least 3 characters, max. 512. */
  reason: string;
  /** Hours until the automatic unban; omit or 0 = permanent. */
  durationHours?: number;
  /** Also ban on every linked Discord server (txBot required; Discord bans are permanent). */
  discordBan?: boolean;
}

export interface KickParams {
  identifier: string;
  reason?: string;
}

export interface DmParams {
  identifier: string;
  message: string;
}

export interface NoteParams {
  identifier: string;
  /** Empty or omitted clears the note. Max. 2000. */
  note?: string;
}

export interface WhitelistParams {
  identifier: string;
  /** `true` = add, `false` = remove. Required. */
  whitelisted: boolean;
}

function filtersParam(filters: PlayerFilter[] | string | undefined): string | undefined {
  if (filters === undefined) return undefined;
  return Array.isArray(filters) ? filters.join(",") : filters;
}

/** Players who are or were connected. */
export class PlayersResource extends Resource {
  /**
   * Fuzzy search by display name; without `query` the full, sorted player list.
   * Max. 100 rows per page — see {@link PlayersResource.searchAll}.
   *
   * Permission: `players.read`. `GET /api/v1/players/search`
   */
  search(params: PlayerSearchParams = {}, options?: RequestOptions): ApiPromise<PlayerPage> {
    return this._t.request(
      "players.search",
      {
        query: {
          query: params.query,
          sortBy: params.sortBy,
          order: params.order,
          filters: filtersParam(params.filters),
          offset: params.offset,
          cursor: params.cursor,
        },
      },
      options,
    );
  }

  /**
   * Auto-paging {@link PlayersResource.search}: iterates every matching player.
   *
   * Permission: `players.read`.
   */
  searchAll(params: PlayerSearchParams = {}, options?: RequestOptions): Paginator<PlayerRow, PlayerPage> {
    return new Paginator<PlayerRow, PlayerPage>(
      (c) => this.search({ ...params, ...c }, options),
      (p) => p.players ?? [],
      (p, rows) => nextPlayerCursor(p, rows, params.sortBy),
      options?.signal,
    );
  }

  /**
   * Currently connected players, by play time descending.
   *
   * Permission: `players.read`. `GET /api/v1/players/online`
   */
  online(params: PlayerCursorParams = {}, options?: RequestOptions): ApiPromise<PlayerPage> {
    return this._t.request("players.online", { query: { offset: params.offset, cursor: params.cursor } }, options);
  }

  /** Auto-paging {@link PlayersResource.online}. Permission: `players.read`. */
  onlineAll(options?: RequestOptions): Paginator<PlayerRow, PlayerPage> {
    return new Paginator<PlayerRow, PlayerPage>(
      (c) => this.online({ ...c }, options),
      (p) => p.players ?? [],
      (p, rows) => nextPlayerCursor(p, rows, "playTime"),
      options?.signal,
    );
  }

  /**
   * Find players by identifier (license / discord / fivem / steam; prefix optional),
   * by play time descending.
   *
   * Permission: `players.read`. `GET /api/v1/players/lookup`
   */
  lookup(identifier: string, params: PlayerCursorParams = {}, options?: RequestOptions): ApiPromise<PlayerPage> {
    return this._t.request(
      "players.lookup",
      { query: { identifier, offset: params.offset, cursor: params.cursor } },
      options,
    );
  }

  /** Auto-paging {@link PlayersResource.lookup}. Permission: `players.read`. */
  lookupAll(identifier: string, options?: RequestOptions): Paginator<PlayerRow, PlayerPage> {
    return new Paginator<PlayerRow, PlayerPage>(
      (c) => this.lookup(identifier, { ...c }, options),
      (p) => p.players ?? [],
      (p, rows) => nextPlayerCursor(p, rows, "playTime"),
      options?.signal,
    );
  }

  /**
   * Full player record as txAdmin returns it (passthrough).
   *
   * Permission: `players.read`. `GET /api/v1/players/info`
   */
  info(identifier: string, options?: RequestOptions): ApiPromise<JsonObject> {
    return this._t.request("players.info", { query: { identifier } }, options);
  }

  /**
   * Player database statistics (txAdmin passthrough).
   *
   * Permission: `players.read`. `GET /api/v1/players/stats`
   */
  stats(options?: RequestOptions): ApiPromise<JsonObject> {
    return this._t.request("players.stats", {}, options);
  }

  /**
   * Warn a player.
   *
   * Permission: `players.write`. `POST /api/v1/players/warn`
   */
  warn(params: WarnParams, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("players.warn", { body: { ...params } }, options);
  }

  /**
   * Ban a player. With `discordBan: true` the result carries a `discord` field with one
   * entry per linked Discord server.
   *
   * Permission: `players.write`. `POST /api/v1/players/ban`
   */
  ban(params: BanParams, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("players.ban", { body: { ...params } }, options);
  }

  /**
   * Kick a player from the server.
   *
   * Permission: `players.write`. `POST /api/v1/players/kick`
   */
  kick(params: KickParams, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("players.kick", { body: { ...params } }, options);
  }

  /**
   * Send a direct message.
   *
   * Permission: `players.write`. `POST /api/v1/players/dm`
   */
  dm(params: DmParams, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("players.dm", { body: { ...params } }, options);
  }

  /**
   * Set (or clear) the staff note of a player.
   *
   * Permission: `players.write`. `POST /api/v1/players/notes`
   */
  setNote(params: NoteParams, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("players.notes", { body: { ...params } }, options);
  }

  /**
   * Add a player to (`whitelisted: true`) or remove from (`false`) the whitelist.
   *
   * Permission: `players.write`. `POST /api/v1/players/whitelist`
   */
  setWhitelisted(params: WhitelistParams, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("players.whitelist", { body: { ...params } }, options);
  }
}
