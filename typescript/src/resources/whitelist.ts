import type { ApiPromise, RequestOptions, Transport } from "../core/http";
import type { MutationResult, WhitelistApproval } from "../types";
import { Resource } from "./base";

/** Identifiers that pass the whitelist on first connect without a request. */
export class WhitelistApprovalsResource extends Resource {
  /** List pre-approved identifiers. Permission: `players.read`. `GET /api/v1/whitelist/approvals` */
  list(options?: RequestOptions): ApiPromise<WhitelistApproval[]> {
    return this._t.request("whitelist.approvals", {}, options);
  }

  /**
   * Pre-approve a typed identifier (`license:`, `license2:`, `discord:`, `fivem:`, `steam:`,
   * `xbl:`, `live:` — never an IP).
   *
   * Permission: `players.write`. `POST /api/v1/whitelist/approvals`
   */
  add(identifier: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("whitelist.approvals.add", { body: { identifier } }, options);
  }

  /** Remove a pre-approval. Permission: `players.write`. `DELETE /api/v1/whitelist/approvals/{identifier}` */
  remove(identifier: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("whitelist.approvals.remove", { path: { identifier } }, options);
  }
}

/** Whitelist requests and pre-approvals. */
export class WhitelistResource extends Resource {
  /** Pre-approved identifiers. */
  readonly approvals: WhitelistApprovalsResource;

  /** @internal */
  constructor(transport: Transport) {
    super(transport);
    this.approvals = new WhitelistApprovalsResource(transport);
  }

  /**
   * Pending whitelist requests (txAdmin passthrough).
   *
   * Permission: `players.read`. `GET /api/v1/whitelist/requests`
   */
  requests(params: { page?: number } = {}, options?: RequestOptions): ApiPromise<unknown> {
    return this._t.request("whitelist.requests", { query: { page: params.page } }, options);
  }

  /** Approve a request. Permission: `players.write`. `POST /api/v1/whitelist/requests/{requestId}/approve` */
  approve(requestId: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("whitelist.approve", { path: { requestId } }, options);
  }

  /**
   * Deny a request. Note: txAdmin confirms this even for an unknown request id.
   *
   * Permission: `players.write`. `POST /api/v1/whitelist/requests/{requestId}/deny`
   */
  deny(requestId: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("whitelist.deny", { path: { requestId } }, options);
  }

  /**
   * Deny every request whose last attempt was at or before `before` (Unix seconds;
   * default now) — requests that arrived after you loaded the list stay.
   *
   * Permission: `players.write`. `POST /api/v1/whitelist/requests/deny-all`
   */
  denyAll(before?: number, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("whitelist.deny-all", { body: { before } }, options);
  }
}
