import type { ApiPromise, RequestOptions } from "../core/http";
import type { AdminCreated, AdminDetails, AdminList, AdminPermissionList, MutationResult } from "../types";
import { Resource } from "./base";

export interface AdminCreateParams {
  /** Login name. */
  name: string;
  /** txAdmin permission keys, at least one (see {@link AdminsResource.availablePermissions}). */
  permissions: string[];
  citizenfxId?: string;
  discordId?: string;
}

/** Only the fields you pass are changed; omitted fields keep their current value. */
export interface AdminEditParams {
  citizenfxId?: string;
  discordId?: string;
  /** Replaces the current permissions. */
  permissions?: string[];
}

/** txAdmin panel admin accounts. */
export class AdminsResource extends Resource {
  /** List panel admins. Permission: `admin.manage`. `GET /api/v1/txadmin/admins` */
  list(options?: RequestOptions): ApiPromise<AdminList> {
    return this._t.request("admins.list", {}, options);
  }

  /**
   * A single admin with its full permission keys. The master account → 403.
   *
   * Permission: `admin.manage`. `GET /api/v1/txadmin/admins/{name}`
   */
  get(name: string, options?: RequestOptions): ApiPromise<AdminDetails> {
    return this._t.request("admins.get", { path: { name } }, options);
  }

  /**
   * Every selectable admin permission (they change between txAdmin versions).
   *
   * Permission: `admin.manage`. `GET /api/v1/txadmin/admins/available-permissions`
   */
  availablePermissions(options?: RequestOptions): ApiPromise<AdminPermissionList> {
    return this._t.request("admins.permissions", {}, options);
  }

  /**
   * Create a panel admin. Returns a one-time generated `password` — store it now.
   *
   * Permission: `admin.manage`. `POST /api/v1/txadmin/admins`
   */
  add(params: AdminCreateParams, options?: RequestOptions): ApiPromise<AdminCreated> {
    return this._t.request("admins.add", { body: { ...params } }, options);
  }

  /**
   * Update an admin. Fields you do not pass are left unchanged.
   *
   * Permission: `admin.manage`. `PUT /api/v1/txadmin/admins/{name}`
   */
  edit(name: string, params: AdminEditParams, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("admins.edit", { path: { name }, body: { ...params } }, options);
  }

  /** Remove a panel admin. Permission: `admin.manage`. `DELETE /api/v1/txadmin/admins/{name}` */
  delete(name: string, options?: RequestOptions): ApiPromise<MutationResult> {
    return this._t.request("admins.delete", { path: { name } }, options);
  }
}
