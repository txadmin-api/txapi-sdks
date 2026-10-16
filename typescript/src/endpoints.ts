/**
 * Internal endpoint table — a mirror of `sdks/spec/endpoints.json` (API 1.8.0).
 * `test/spec-sync.test.ts` fails when it drifts from the manifest.
 * @internal
 */

export type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

/** @internal */
export interface FieldDef {
  name: string;
  type: "string" | "integer" | "boolean" | "string[]";
  required: boolean;
  maxLength?: number;
}

/** @internal */
export interface EndpointDef {
  method: HttpMethod;
  path: string;
  permission: string | null;
  accountLevel: boolean;
  /** Required query/path parameters (validated client-side). */
  requiredParams: readonly string[];
  /** Body fields; empty = the request is sent without a body. */
  body: readonly FieldDef[];
}

/** @internal */
export const ENDPOINTS = {
  "account.me": { method: "GET", path: "/api/v1/me", permission: null, accountLevel: true, requiredParams: [], body: [] },
  "account.servers": { method: "GET", path: "/api/v1/servers", permission: null, accountLevel: true, requiredParams: [], body: [] },
  "players.search": { method: "GET", path: "/api/v1/players/search", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "players.online": { method: "GET", path: "/api/v1/players/online", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "players.lookup": { method: "GET", path: "/api/v1/players/lookup", permission: "players.read", accountLevel: false, requiredParams: ["identifier"], body: [] },
  "players.info": { method: "GET", path: "/api/v1/players/info", permission: "players.read", accountLevel: false, requiredParams: ["identifier"], body: [] },
  "players.stats": { method: "GET", path: "/api/v1/players/stats", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "players.warn": { method: "POST", path: "/api/v1/players/warn", permission: "players.write", accountLevel: false, requiredParams: [], body: [{name: "identifier", type: "string", required: true}, {name: "reason", type: "string", required: true, maxLength: 512}] },
  "players.ban": { method: "POST", path: "/api/v1/players/ban", permission: "players.write", accountLevel: false, requiredParams: [], body: [{name: "identifier", type: "string", required: true}, {name: "reason", type: "string", required: true, maxLength: 512}, {name: "durationHours", type: "integer", required: false}, {name: "discordBan", type: "boolean", required: false}] },
  "players.kick": { method: "POST", path: "/api/v1/players/kick", permission: "players.write", accountLevel: false, requiredParams: [], body: [{name: "identifier", type: "string", required: true}, {name: "reason", type: "string", required: false, maxLength: 512}] },
  "players.dm": { method: "POST", path: "/api/v1/players/dm", permission: "players.write", accountLevel: false, requiredParams: [], body: [{name: "identifier", type: "string", required: true}, {name: "message", type: "string", required: true, maxLength: 512}] },
  "players.notes": { method: "POST", path: "/api/v1/players/notes", permission: "players.write", accountLevel: false, requiredParams: [], body: [{name: "identifier", type: "string", required: true}, {name: "note", type: "string", required: false, maxLength: 2000}] },
  "players.whitelist": { method: "POST", path: "/api/v1/players/whitelist", permission: "players.write", accountLevel: false, requiredParams: [], body: [{name: "identifier", type: "string", required: true}, {name: "whitelisted", type: "boolean", required: true}] },
  "history.bans": { method: "GET", path: "/api/v1/history/bans", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "history.warns": { method: "GET", path: "/api/v1/history/warns", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "history.search": { method: "GET", path: "/api/v1/history/search", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "history.ban-templates": { method: "GET", path: "/api/v1/txadmin/ban-templates", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "history.action": { method: "GET", path: "/api/v1/history/action/{actionId}", permission: "players.read", accountLevel: false, requiredParams: ["actionId"], body: [] },
  "history.stats": { method: "GET", path: "/api/v1/history/stats", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "history.revoke": { method: "POST", path: "/api/v1/history/revoke/{actionId}", permission: "players.write", accountLevel: false, requiredParams: ["actionId"], body: [{name: "reason", type: "string", required: false, maxLength: 512}, {name: "discordUnban", type: "boolean", required: false}] },
  "server.status": { method: "GET", path: "/api/v1/server/status", permission: "server.control", accountLevel: false, requiredParams: [], body: [] },
  "server.diagnostics": { method: "GET", path: "/api/v1/server/diagnostics", permission: "server.control", accountLevel: false, requiredParams: [], body: [] },
  "server.drops": { method: "GET", path: "/api/v1/server/drops", permission: "server.control", accountLevel: false, requiredParams: [], body: [] },
  "server.performance": { method: "GET", path: "/api/v1/server/performance", permission: "server.control", accountLevel: false, requiredParams: [], body: [] },
  "server.restart": { method: "POST", path: "/api/v1/server/restart", permission: "server.control", accountLevel: false, requiredParams: [], body: [] },
  "server.start": { method: "POST", path: "/api/v1/server/start", permission: "server.control", accountLevel: false, requiredParams: [], body: [] },
  "server.stop": { method: "POST", path: "/api/v1/server/stop", permission: "server.control", accountLevel: false, requiredParams: [], body: [] },
  "server.schedule-restart": { method: "POST", path: "/api/v1/server/schedule-restart", permission: "server.control", accountLevel: false, requiredParams: [], body: [{name: "time", type: "string", required: true}] },
  "server.skip-restart": { method: "POST", path: "/api/v1/server/skip-restart", permission: "server.control", accountLevel: false, requiredParams: [], body: [{name: "skip", type: "boolean", required: true}] },
  "server.kickall": { method: "POST", path: "/api/v1/server/kickall", permission: "server.control", accountLevel: false, requiredParams: [], body: [{name: "reason", type: "string", required: false, maxLength: 512}] },
  "server.announcement": { method: "POST", path: "/api/v1/server/announcement", permission: "server.control", accountLevel: false, requiredParams: [], body: [{name: "message", type: "string", required: true, maxLength: 512}] },
  "server.resource.start": { method: "POST", path: "/api/v1/server/resource/start", permission: "server.control", accountLevel: false, requiredParams: [], body: [{name: "name", type: "string", required: true, maxLength: 128}] },
  "server.resource.stop": { method: "POST", path: "/api/v1/server/resource/stop", permission: "server.control", accountLevel: false, requiredParams: [], body: [{name: "name", type: "string", required: true, maxLength: 128}] },
  "server.resource.restart": { method: "POST", path: "/api/v1/server/resource/restart", permission: "server.control", accountLevel: false, requiredParams: [], body: [{name: "name", type: "string", required: true, maxLength: 128}] },
  "server.resource.ensure": { method: "POST", path: "/api/v1/server/resource/ensure", permission: "server.control", accountLevel: false, requiredParams: [], body: [{name: "name", type: "string", required: true, maxLength: 128}] },
  "server.resource.refresh": { method: "POST", path: "/api/v1/server/resource/refresh", permission: "server.control", accountLevel: false, requiredParams: [], body: [] },
  "console.buffer": { method: "GET", path: "/api/v1/console", permission: "console.write", accountLevel: false, requiredParams: [], body: [] },
  "console.command": { method: "POST", path: "/api/v1/console/command", permission: "console.write", accountLevel: false, requiredParams: [], body: [{name: "command", type: "string", required: true, maxLength: 1024}] },
  "serverlog.buffer": { method: "GET", path: "/api/v1/server/log", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "serverlog.history": { method: "GET", path: "/api/v1/server/log/history", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.list": { method: "GET", path: "/api/v1/events", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.deaths": { method: "GET", path: "/api/v1/events/deaths", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.kills": { method: "GET", path: "/api/v1/events/kills", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.leaderboard": { method: "GET", path: "/api/v1/events/leaderboard", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.player": { method: "GET", path: "/api/v1/events/player", permission: "logs.view", accountLevel: false, requiredParams: ["player"], body: [] },
  "events.summary": { method: "GET", path: "/api/v1/events/summary", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.chat": { method: "GET", path: "/api/v1/events/chat", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.commands": { method: "GET", path: "/api/v1/events/commands", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.admin-actions": { method: "GET", path: "/api/v1/events/admin-actions", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.actions": { method: "GET", path: "/api/v1/events/actions", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.sessions": { method: "GET", path: "/api/v1/events/sessions", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.playtime": { method: "GET", path: "/api/v1/events/playtime", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.activity": { method: "GET", path: "/api/v1/events/activity", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "events.action-log": { method: "GET", path: "/api/v1/txadmin/action-log", permission: "logs.view", accountLevel: false, requiredParams: [], body: [] },
  "whitelist.requests": { method: "GET", path: "/api/v1/whitelist/requests", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "whitelist.approve": { method: "POST", path: "/api/v1/whitelist/requests/{requestId}/approve", permission: "players.write", accountLevel: false, requiredParams: ["requestId"], body: [] },
  "whitelist.deny": { method: "POST", path: "/api/v1/whitelist/requests/{requestId}/deny", permission: "players.write", accountLevel: false, requiredParams: ["requestId"], body: [] },
  "whitelist.deny-all": { method: "POST", path: "/api/v1/whitelist/requests/deny-all", permission: "players.write", accountLevel: false, requiredParams: [], body: [{name: "before", type: "integer", required: false}] },
  "whitelist.approvals": { method: "GET", path: "/api/v1/whitelist/approvals", permission: "players.read", accountLevel: false, requiredParams: [], body: [] },
  "whitelist.approvals.add": { method: "POST", path: "/api/v1/whitelist/approvals", permission: "players.write", accountLevel: false, requiredParams: [], body: [{name: "identifier", type: "string", required: true}] },
  "whitelist.approvals.remove": { method: "DELETE", path: "/api/v1/whitelist/approvals/{identifier}", permission: "players.write", accountLevel: false, requiredParams: ["identifier"], body: [] },
  "admins.list": { method: "GET", path: "/api/v1/txadmin/admins", permission: "admin.manage", accountLevel: false, requiredParams: [], body: [] },
  "admins.get": { method: "GET", path: "/api/v1/txadmin/admins/{name}", permission: "admin.manage", accountLevel: false, requiredParams: ["name"], body: [] },
  "admins.permissions": { method: "GET", path: "/api/v1/txadmin/admins/available-permissions", permission: "admin.manage", accountLevel: false, requiredParams: [], body: [] },
  "admins.add": { method: "POST", path: "/api/v1/txadmin/admins", permission: "admin.manage", accountLevel: false, requiredParams: [], body: [{name: "name", type: "string", required: true}, {name: "citizenfxId", type: "string", required: false}, {name: "discordId", type: "string", required: false}, {name: "permissions", type: "string[]", required: true}] },
  "admins.edit": { method: "PUT", path: "/api/v1/txadmin/admins/{name}", permission: "admin.manage", accountLevel: false, requiredParams: ["name"], body: [{name: "citizenfxId", type: "string", required: false}, {name: "discordId", type: "string", required: false}, {name: "permissions", type: "string[]", required: false}] },
  "admins.delete": { method: "DELETE", path: "/api/v1/txadmin/admins/{name}", permission: "admin.manage", accountLevel: false, requiredParams: ["name"], body: [] },
} as const satisfies Record<string, EndpointDef>;

/** @internal */
export type EndpointId = keyof typeof ENDPOINTS;
