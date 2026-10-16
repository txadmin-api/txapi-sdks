import { afterAll, beforeAll, describe, expect, it } from "vitest";
import type { TxApi } from "../src";
import { client, loadManifest, startMock, type MockServer } from "./helpers";

/**
 * Every endpoint called with EVERY parameter and body field the API accepts.
 * The test then checks the wire: each query parameter and body field of
 * spec/endpoints.json arrives under exactly its name — nothing missing, nothing extra.
 */
const SINCE = "7d";
const CALLS: Array<[string, (c: TxApi) => Promise<unknown>]> = [
  ["account.me", (c) => c.account.me()],
  ["account.servers", (c) => c.account.servers()],
  ["players.search", (c) => c.players.search({ query: "john", sortBy: "tsJoined", order: "asc", filters: ["isOnline", "hasNote"], offset: 5, cursor: "lic" })],
  ["players.online", (c) => c.players.online({ offset: 5, cursor: "lic" })],
  ["players.lookup", (c) => c.players.lookup("discord:1", { offset: 5, cursor: "lic" })],
  ["players.info", (c) => c.players.info("license:abc")],
  ["players.stats", (c) => c.players.stats()],
  ["players.warn", (c) => c.players.warn({ identifier: "license:abc", reason: "spam" })],
  ["players.ban", (c) => c.players.ban({ identifier: "license:abc", reason: "cheating", durationHours: 24, discordBan: true })],
  ["players.kick", (c) => c.players.kick({ identifier: "license:abc", reason: "afk" })],
  ["players.dm", (c) => c.players.dm({ identifier: "license:abc", message: "hi" })],
  ["players.notes", (c) => c.players.setNote({ identifier: "license:abc", note: "watch" })],
  ["players.whitelist", (c) => c.players.setWhitelisted({ identifier: "license:abc", whitelisted: false })],
  ["history.bans", (c) => c.history.bans({ order: "asc", offset: 1, cursor: "A" })],
  ["history.warns", (c) => c.history.warns({ order: "asc", offset: 1, cursor: "A" })],
  ["history.search", (c) => c.history.search({ type: "ban", admin: "x", searchType: "reason", search: "y", order: "asc", offset: 1, cursor: "A" })],
  ["history.ban-templates", (c) => c.history.banTemplates()],
  ["history.action", (c) => c.history.action("BKL5-3SH4")],
  ["history.stats", (c) => c.history.stats()],
  ["history.revoke", (c) => c.history.revoke("BKL5-3SH4", { reason: "oops", discordUnban: true })],
  ["server.status", (c) => c.server.status()],
  ["server.diagnostics", (c) => c.server.diagnostics()],
  ["server.drops", (c) => c.server.drops()],
  ["server.performance", (c) => c.server.performance({ thread: "svMain" })],
  ["server.restart", (c) => c.server.restart()],
  ["server.start", (c) => c.server.start()],
  ["server.stop", (c) => c.server.stop()],
  ["server.schedule-restart", (c) => c.server.scheduleRestart("+30")],
  ["server.skip-restart", (c) => c.server.skipRestart(false)],
  ["server.kickall", (c) => c.server.kickAll("maintenance")],
  ["server.announcement", (c) => c.server.announce("Restart in 5")],
  ["server.resource.start", (c) => c.server.resources.start("chat")],
  ["server.resource.stop", (c) => c.server.resources.stop("chat")],
  ["server.resource.restart", (c) => c.server.resources.restart("chat")],
  ["server.resource.ensure", (c) => c.server.resources.ensure("chat")],
  ["server.resource.refresh", (c) => c.server.resources.refresh()],
  ["console.buffer", (c) => c.console.buffer()],
  ["console.command", (c) => c.console.command("status")],
  ["serverlog.buffer", (c) => c.serverlog.buffer()],
  ["serverlog.history", (c) => c.serverlog.history({ direction: "newer", ref: 1700000000000 })],
  ["events.list", (c) => c.events.list({ type: ["death", "ban"], player: "p", search: "s", since: SINCE, until: "1h", cursor: "1_2", limit: 10 })],
  ["events.deaths", (c) => c.events.deaths({ player: "p", victim: "v", killer: "k", cause: "c", pvp: false, since: SINCE, until: "1h", cursor: "1_2", limit: 10 })],
  ["events.kills", (c) => c.events.kills({ player: "p", killer: "k", victim: "v", cause: "c", since: SINCE, until: "1h", cursor: "1_2", limit: 10 })],
  ["events.leaderboard", (c) => c.events.leaderboard({ since: SINCE, sort: "kd", limit: 10 })],
  ["events.player", (c) => c.events.player("Jane", { since: SINCE })],
  ["events.summary", (c) => c.events.summary({ since: SINCE })],
  ["events.chat", (c) => c.events.chat({ player: "p", search: "s", since: SINCE, until: "1h", cursor: "1_2", limit: 10 })],
  ["events.commands", (c) => c.events.commands({ player: "p", search: "s", since: SINCE, until: "1h", cursor: "1_2", limit: 10 })],
  ["events.admin-actions", (c) => c.events.adminActions({ admin: "a", kind: "ban", search: "s", since: SINCE, until: "1h", cursor: "1_2", limit: 10 })],
  ["events.actions", (c) => c.events.actions({ type: "warn", player: "p", admin: "a", revoked: false, since: SINCE, until: "1h", cursor: "1_2", limit: 10 })],
  ["events.sessions", (c) => c.events.sessions({ player: "p", since: SINCE, until: "1h", cursor: "1_2", limit: 10 })],
  ["events.playtime", (c) => c.events.playtime({ since: SINCE, player: "p", limit: 10 })],
  ["events.activity", (c) => c.events.activity({ since: SINCE, bucket: "day", timeZone: "Europe/Vienna" })],
  ["events.action-log", (c) => c.events.actionLog({ lines: 10 })],
  ["whitelist.requests", (c) => c.whitelist.requests({ page: 2 })],
  ["whitelist.approve", (c) => c.whitelist.approve("R1234")],
  ["whitelist.deny", (c) => c.whitelist.deny("R1234")],
  ["whitelist.deny-all", (c) => c.whitelist.denyAll(1700000000)],
  ["whitelist.approvals", (c) => c.whitelist.approvals.list()],
  ["whitelist.approvals.add", (c) => c.whitelist.approvals.add("discord:1")],
  ["whitelist.approvals.remove", (c) => c.whitelist.approvals.remove("discord:1")],
  ["admins.list", (c) => c.admins.list()],
  ["admins.get", (c) => c.admins.get("moderator 1")],
  ["admins.permissions", (c) => c.admins.availablePermissions()],
  ["admins.add", (c) => c.admins.add({ name: "mod", permissions: ["console.view"], citizenfxId: "fivem:1", discordId: "discord:2" })],
  ["admins.edit", (c) => c.admins.edit("mod", { permissions: ["players.ban"], citizenfxId: "fivem:1", discordId: "discord:2" })],
  ["admins.delete", (c) => c.admins.delete("mod")],
];

describe("full parameter coverage", () => {
  let server: MockServer;
  const manifest = loadManifest();
  beforeAll(async () => {
    server = await startMock();
  });
  afterAll(() => server.close());

  it("covers every endpoint of the manifest", () => {
    expect(CALLS.map(([id]) => id).sort()).toEqual(manifest.endpoints.map((e) => e.id).sort());
  });

  it("sends every query parameter and body field under its wire name", async () => {
    const c = client(server, { serverId: 7 });
    for (const [id, call] of CALLS) {
      const def = manifest.endpoints.find((e) => e.id === id)!;
      const before = server.requests.length;
      await call(c);
      const req = server.requests[before]!;
      const query = [...req.query.keys()].filter((k) => k !== "serverId").sort();
      const wantQuery = (def.params ?? []).filter((p) => p.in === "query").map((p) => p.name).sort();
      expect(query, `${id} query`).toEqual(wantQuery);
      const wantBody = (def.body ?? []).map((b) => b.name).sort();
      const body = req.body && typeof req.body === "object" ? Object.keys(req.body as object).sort() : [];
      expect(body, `${id} body`).toEqual(wantBody);
      for (const p of (def.params ?? []).filter((x) => x.in === "path")) {
        expect(def.path.includes(`{${p.name}}`), `${id} path ${p.name}`).toBe(true);
      }
    }
  });
});
