import type { ApiPromise, RequestOptions } from "../core/http";
import type { AccountInfo, ServerList } from "../types";
import { Resource } from "./base";

/** Account-level calls: no txAdmin request, no plan feature, no `serverId`. */
export class AccountResource extends Resource {
  /**
   * Your account: plan code, effective features (plan ∩ key scopes), monthly quota,
   * per-minute limit and the key in use. Counts against the quota like every call.
   *
   * Permission: any valid key. `GET /api/v1/me`
   */
  me(options?: RequestOptions): ApiPromise<AccountInfo> {
    return this._t.request("account.me", {}, options);
  }

  /**
   * Servers reachable with this key (use their `id` as `serverId`). No URLs, no credentials.
   *
   * Permission: any valid key. `GET /api/v1/servers`
   */
  servers(options?: RequestOptions): ApiPromise<ServerList> {
    return this._t.request("account.servers", {}, options);
  }
}
