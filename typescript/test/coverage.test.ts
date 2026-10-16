import { afterAll, beforeAll, describe, expect, it } from "vitest";
import type { TxApi } from "../src";
import { client, loadManifest, startMock, templateFor, type MockServer } from "./helpers";

/** One call per public endpoint method. */
const CALLS: Array<[string, (c: TxApi) => Promise<unknown>]> = [
  ["account.me", (c) => c.account.me()],
  ["account.servers", (c) => c.account.servers()],
  ["players.search", (c) => c.players.search({ query: "john" })],
  ["players.online", (c) => c.players.online()],
  ["players.lookup", (c) => c.players.lookup("discord:1")],
  ["players.info", (c) => c.players.info("license:abc")],
  ["players.stats", (c) => c.players.stats()],
  ["players.warn", (c) => c.players.warn({ identifier: "license:abc", reason: "spam" })],
  ["players.ban", (c) => c.players.ban({ identifier: "license:abc", reason: "cheating" })],
  ["players.kick", (c) => c.players.kick({ identifier: "license:abc" })],
  ["players.dm", (c) => c.players.dm({ identifier: "license:abc", message: "hi" })],
  ["players.notes", (c) => c.players.setNote({ identifier: "license:abc", note: "watch" })],
  ["players.whitelist", (c) => c.players.setWhitelisted({ identifier: "license:abc", whitelisted: true })],
  ["history.bans", (c) => c.history.bans()],
  ["history.warns", (c) => c.history.warns()],
  ["history.search", (c) => c.history.search({ type: "ban" })],
  ["history.ban-templates", (c) => c.history.banTemplates()],
  ["history.action", (c) => c.history.action("BKL5-3SH4")],
  ["history.stats", (c) => c.history.stats()],
  ["history.revoke", (c) => c.history.revoke("BKL5-3SH4", { reason: "oops" })],
  ["server.status", (c) => c.server.status()],
  ["server.diagnostics", (c) => c.server.diagnostics()],
  ["server.drops", (c) => c.server.drops()],
  ["server.performance", (c) => c.server.performance({ thread: "svMain" })],
  ["server.restart", (c) => c.server.restart()],
  ["server.start", (c) => c.server.start()],
  ["server.stop", (c) => c.server.stop()],
  ["server.schedule-restart", (c) => c.server.scheduleRestart("+30")],
  ["server.skip-restart", (c) => c.server.skipRestart(true)],
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
  ["serverlog.history", (c) => c.serverlog.history({ direction: "older" })],
  ["events.list", (c) => c.events.list({ type: ["death", "ban"] })],
  ["events.deaths", (c) => c.events.deaths({ pvp: true })],
  ["events.kills", (c) => c.events.kills()],
  ["events.leaderboard", (c) => c.events.leaderboard({ sort: "kd" })],
  ["events.player", (c) => c.events.player("Jane")],
  ["events.summary", (c) => c.events.summary()],
  ["events.chat", (c) => c.events.chat()],
  ["events.commands", (c) => c.events.commands()],
  ["events.admin-actions", (c) => c.events.adminActions({ kind: "ban" })],
  ["events.actions", (c) => c.events.actions({ revoked: false })],
  ["events.sessions", (c) => c.events.sessions()],
  ["events.playtime", (c) => c.events.playtime()],
  ["events.activity", (c) => c.events.activity({ bucket: "day" })],
  ["events.action-log", (c) => c.events.actionLog({ lines: 10 })],
  ["whitelist.requests", (c) => c.whitelist.requests({ page: 1 })],
  ["whitelist.approve", (c) => c.whitelist.approve("R1234")],
  ["whitelist.deny", (c) => c.whitelist.deny("R1234")],
  ["whitelist.deny-all", (c) => c.whitelist.denyAll()],
  ["whitelist.approvals", (c) => c.whitelist.approvals.list()],
  ["whitelist.approvals.add", (c) => c.whitelist.approvals.add("discord:1")],
  ["whitelist.approvals.remove", (c) => c.whitelist.approvals.remove("discord:1")],
  ["admins.list", (c) => c.admins.list()],
  ["admins.get", (c) => c.admins.get("moderator 1")],
  ["admins.permissions", (c) => c.admins.availablePermissions()],
  ["admins.add", (c) => c.admins.add({ name: "mod", permissions: ["console.view"] })],
  ["admins.edit", (c) => c.admins.edit("mod", { permissions: ["players.ban"] })],
  ["admins.delete", (c) => c.admins.delete("mod")],
];

describe("coverage", () => {
  let server: MockServer;
  const manifest = loadManifest();

  beforeAll(async () => {
    server = await startMock();
  });
  afterAll(() => server.close());

  it("has one method per endpoint in spec/endpoints.json and hits exactly those routes", async () => {
    const c = client(server);
    const recordedById = new Map<string, string | undefined>();
    for (const [id, call] of CALLS) {
      const before = server.requests.length;
      await call(c);
      expect(server.requests.length, `${id} sent exactly one request`).toBe(before + 1);
      const req = server.requests[before]!;
      recordedById.set(id, templateFor(manifest, req.method, req.rawPath));
    }

    const recorded = new Set([...recordedById.values()]);
    const expected = new Set(manifest.endpoints.map((e) => `${e.method} ${e.path}`));
    expect(recorded.has(undefined)).toBe(false);
    expect([...recorded].sort()).toEqual([...expected].sort());
    expect(expected.size).toBe(67);

    // Each call hit the template of the endpoint it claims to implement.
    for (const e of manifest.endpoints) {
      expect(recordedById.get(e.id), e.id).toBe(`${e.method} ${e.path}`);
    }
  });

  it("sends serverId on every server-scoped endpoint and never on account-level ones", async () => {
    const c = client(server, { serverId: 7 });
    for (const [id, call] of CALLS) {
      const before = server.requests.length;
      await call(c);
      const req = server.requests[before]!;
      const def = manifest.endpoints.find((e) => e.id === id)!;
      if (def.accountLevel) expect(req.query.has("serverId"), id).toBe(false);
      else expect(req.query.get("serverId"), id).toBe("7");
    }
  });
});
