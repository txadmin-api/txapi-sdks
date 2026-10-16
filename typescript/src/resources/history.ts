import type { ApiPromise, RequestOptions } from "../core/http";
import { Paginator, nextHistoryCursor } from "../core/pagination";
import type { BanTemplate, HistoryPage, HistoryRow, JsonObject, MutationResult } from "../types";
import { Resource } from "./base";
import type { SortOrder } from "./players";

/** Cursor parameters of a history list: `offset` = `timestamp`, `cursor` = `id` of the last row. */
export interface HistoryListParams {
  /** By `timestamp`, default `desc`. */
  order?: SortOrder;
  offset?: number;
  cursor?: string;
}

export interface HistorySearchParams extends HistoryListParams {
  /** `ban` | `warn`; omit for both. */
  type?: "ban" | "warn";
  /** Name of the txAdmin admin who issued the action. */
  admin?: string;
  /** `actionId` | `reason` | `identifiers` (default `reason` when `search` is set). */
  searchType?: "actionId" | "reason" | "identifiers";
  /** Search value; comma-separated for `identifiers`. */
  search?: string;
}

export interface RevokeParams {
  /** Audit reason, max. 512. */
  reason?: string;
  /** Also lift the ban on the linked Discord servers (txBot required). */
  discordUnban?: boolean;
}

/** Past moderation actions (bans and warns). */
export class HistoryResource extends Resource {
  /**
   * Bans, cursor-paginated (max. 100 per page).
   *
   * Permission: `players.read`. `GET /api/v1/history/bans`
   */
  bans(params: HistoryListParams = {}, options?: RequestOptions): ApiPromise<HistoryPage> {
    return this._t.request("history.bans", { query: { ...params } }, options);
  }

  /** Auto-paging {@link HistoryResource.bans}. Permission: `players.read`. */
  bansAll(params: Omit<HistoryListParams, "offset" | "cursor"> = {}, options?: RequestOptions): Paginator<HistoryRow, HistoryPage> {
    return this.#paginate((c) => this.bans({ ...params, ...c }, options), options);
  }

  /**
   * Warns, cursor-paginated (max. 100 per page).
   *
   * Permission: `players.read`. `GET /api/v1/history/warns`
   */
  warns(params: HistoryListParams = {}, options?: RequestOptions): ApiPromise<HistoryPage> {
    return this._t.request("history.warns", { query: { ...params } }, options);
  }

  /** Auto-paging {@link HistoryResource.warns}. Permission: `players.read`. */
  warnsAll(params: Omit<HistoryListParams, "offset" | "cursor"> = {}, options?: RequestOptions): Paginator<HistoryRow, HistoryPage> {
    return this.#paginate((c) => this.warns({ ...params, ...c }, options), options);
  }

  /**
   * Filter and search bans and warns (type, admin, action id, reason, identifiers).
   *
   * Permission: `players.read`. `GET /api/v1/history/search`
   */
  search(params: HistorySearchParams = {}, options?: RequestOptions): ApiPromise<HistoryPage> {
    return this._t.request("history.search", { query: { ...params } }, options);
  }

  /** Auto-paging {@link HistoryResource.search}. Permission: `players.read`. */
  searchAll(params: Omit<HistorySearchParams, "offset" | "cursor"> = {}, options?: RequestOptions): Paginator<HistoryRow, HistoryPage> {
    return this.#paginate((c) => this.search({ ...params, ...c }, options), options);
  }

  /**
   * Ban templates configured in txAdmin (reason + duration).
   *
   * Permission: `players.read`. `GET /api/v1/txadmin/ban-templates`
   */
  banTemplates(options?: RequestOptions): ApiPromise<BanTemplate[]> {
    return this._t.request("history.ban-templates", {}, options);
  }

  /**
   * A single action by id (txAdmin passthrough).
   *
   * Permission: `players.read`. `GET /api/v1/history/action/{actionId}`
   */
  action(actionId: string, options?: RequestOptions): ApiPromise<JsonObject> {
    return this._t.request("history.action", { path: { actionId } }, options);
  }

  /**
   * Number of bans / warns (txAdmin passthrough).
   *
   * Permission: `players.read`. `GET /api/v1/history/stats`
   */
  stats(options?: RequestOptions): ApiPromise<JsonObject> {
    return this._t.request("history.stats", {}, options);
  }

  /**
   * Revoke a ban or warn. With `discordUnban: true` the result carries a `discord` field.
   *
   * Permission: `players.write`. `POST /api/v1/history/revoke/{actionId}`
   */
  revoke(actionId: string, params: RevokeParams = {}, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("history.revoke", { path: { actionId }, body: { ...params } }, options);
  }

  #paginate(
    fetchPage: (c: Record<string, unknown> | undefined) => Promise<HistoryPage>,
    options: RequestOptions | undefined,
  ): Paginator<HistoryRow, HistoryPage> {
    return new Paginator<HistoryRow, HistoryPage>(
      fetchPage,
      (p) => p.history ?? [],
      (p, rows) => nextHistoryCursor(p, rows),
      options?.signal,
    );
  }
}
