import { afterAll, beforeAll, describe, expect, it, vi } from "vitest";
import { inspect } from "node:util";
import { TxApi, ValidationError, VERSION } from "../src";
import { client, startMock, type MockServer } from "./helpers";

describe("request building", () => {
  let server: MockServer;
  beforeAll(async () => {
    server = await startMock();
  });
  afterAll(() => server.close());

  const last = () => server.requests[server.requests.length - 1]!;

  it("sends the key as Bearer token, JSON accept and a versioned user agent", async () => {
    const c = client(server, { userAgent: "my-bot/2.0" });
    await c.players.stats();
    expect(last().headers.authorization).toBe("Bearer txp_test_SECRETKEY");
    expect(last().headers.accept).toBe("application/json");
    expect(last().headers["user-agent"]).toBe(`txapi-typescript/${VERSION} my-bot/2.0`);
  });

  it("percent-encodes path parameters", async () => {
    const c = client(server);
    await c.whitelist.approvals.remove("license:abc");
    expect(last().rawPath).toBe("/api/v1/whitelist/approvals/license%3Aabc");
    await c.admins.get("name with space");
    expect(last().rawPath).toBe("/api/v1/txadmin/admins/name%20with%20space");
    await c.history.revoke("A/B");
    expect(last().rawPath).toBe("/api/v1/history/revoke/A%2FB");
  });

  it("omits serverId on account-level endpoints and lets the per-call value win", async () => {
    const c = client(server, { serverId: 3 });
    await c.account.me({ serverId: 9 });
    expect(last().query.has("serverId")).toBe(false);
    await c.account.servers();
    expect(last().query.has("serverId")).toBe(false);
    await c.server.status();
    expect(last().query.get("serverId")).toBe("3");
    await c.server.status({ serverId: 9 });
    expect(last().query.get("serverId")).toBe("9");
  });

  it("omits serverId entirely without a default", async () => {
    const c = new TxApi({ apiKey: "k", baseUrl: server.url, maxRetries: 0 });
    await c.server.status();
    expect(last().query.has("serverId")).toBe(false);
  });

  it("omits null/undefined query values and serialises booleans and arrays", async () => {
    const c = client(server);
    await c.players.search({ query: undefined, sortBy: "tsJoined", filters: ["isOnline", "hasNote"] });
    expect(last().query.has("query")).toBe(false);
    expect(last().query.get("sortBy")).toBe("tsJoined");
    expect(last().query.get("filters")).toBe("isOnline,hasNote");
    await c.events.deaths({ pvp: false, since: new Date("2026-01-02T03:04:05Z"), until: undefined });
    expect(last().query.get("pvp")).toBe("false");
    expect(last().query.get("since")).toBe("2026-01-02T03:04:05.000Z");
    expect(last().query.has("until")).toBe(false);
    await c.events.actions({ revoked: true });
    expect(last().query.get("revoked")).toBe("true");
  });

  it("omits absent and null body fields, keeps false and 0", async () => {
    const c = client(server);
    await c.admins.edit("mod", { discordId: "123", citizenfxId: undefined, permissions: null as unknown as undefined });
    expect(last().method).toBe("PUT");
    expect(last().body).toEqual({ discordId: "123" });
    expect(last().bodyText).not.toContain("null");
    expect(last().headers["content-type"]).toBe("application/json");

    await c.players.setWhitelisted({ identifier: "license:x", whitelisted: false });
    expect(last().body).toEqual({ identifier: "license:x", whitelisted: false });
    await c.players.ban({ identifier: "license:x", reason: "cheating", durationHours: 0, discordBan: true });
    expect(last().body).toEqual({ identifier: "license:x", reason: "cheating", durationHours: 0, discordBan: true });
    await c.server.skipRestart(false);
    expect(last().body).toEqual({ skip: false });
  });

  it("sends no body for endpoints without body fields and {} for empty optional bodies", async () => {
    const c = client(server);
    await c.server.restart();
    expect(last().bodyText).toBe("");
    expect(last().headers["content-type"]).toBeUndefined();
    await c.whitelist.denyAll();
    expect(last().bodyText).toBe("{}");
  });

  it("validates required fields and maxLength without any network call", async () => {
    const c = client(server);
    const before = server.requests.length;
    await expect(c.players.ban({ identifier: "", reason: "cheating" })).rejects.toBeInstanceOf(ValidationError);
    await expect(c.players.ban({ identifier: "license:x", reason: "   " })).rejects.toThrow(/reason/);
    await expect(c.players.ban({ identifier: "license:x" } as never)).rejects.toThrow(/`reason` is required/);
    await expect(c.players.ban({ identifier: "license:x", reason: "x".repeat(513) })).rejects.toThrow(/at most 512/);
    await expect(c.players.setNote({ identifier: "license:x", note: "n".repeat(2001) })).rejects.toThrow(/2000/);
    await expect(c.players.setWhitelisted({ identifier: "license:x" } as never)).rejects.toThrow(/whitelisted/);
    await expect(c.console.command("c".repeat(1025))).rejects.toThrow(/1024/);
    await expect(c.server.resources.restart("r".repeat(129))).rejects.toThrow(/128/);
    await expect(c.admins.add({ name: "mod", permissions: [] })).rejects.toThrow(/permissions/);
    await expect(c.players.lookup("")).rejects.toThrow(/identifier/);
    await expect(c.events.player(" ")).rejects.toThrow(/player/);
    await expect(c.history.action("")).rejects.toThrow(/actionId/);
    const err = await c.players.ban({ identifier: "license:x", reason: "x".repeat(600) }).catch((e: unknown) => e);
    expect((err as ValidationError).field).toBe("reason");
    expect(server.requests.length).toBe(before);
    // exactly 512 is fine
    await c.players.ban({ identifier: "license:x", reason: "x".repeat(512) });
    expect(server.requests.length).toBe(before + 1);
  });

  it("strips trailing slashes and a trailing /api/v1 from the base URL", async () => {
    const c = new TxApi({ apiKey: "k", baseUrl: `${server.url}/api/v1/`, maxRetries: 0 });
    await c.account.me();
    expect(last().rawPath).toBe("/api/v1/me");
  });

  it("requires a key, falls back to the environment, and never prints it", async () => {
    vi.stubEnv("TXAPI_KEY", "");
    expect(() => new TxApi()).toThrow(ValidationError);
    vi.stubEnv("TXAPI_KEY", "txp_from_env");
    vi.stubEnv("TXAPI_BASE_URL", server.url);
    const c = new TxApi({ maxRetries: 0 });
    await c.account.me();
    expect(last().headers.authorization).toBe("Bearer txp_from_env");
    vi.unstubAllEnvs();

    const secret = client(server);
    expect(String(secret)).not.toContain("SECRETKEY");
    expect(JSON.stringify(secret)).not.toContain("SECRETKEY");
    expect(inspect(secret, { depth: 10, showHidden: true })).not.toContain("SECRETKEY");
  });

  it("returns data and the raw response via withResponse()", async () => {
    const c = client(server);
    const { data, response } = await c.account.me().withResponse();
    expect(data).toEqual({ success: true, message: "ok" });
    expect(response.status).toBe(200);
    expect(response.headers.get("x-ratelimit-limit")).toBe("120");
    expect(response.rateLimit).toEqual({ limit: 120, remaining: 119, resetSeconds: 42 });
  });

  it("calls hooks and survives hooks that throw", async () => {
    const seen: string[] = [];
    const c = client(server, {
      onRequest: (i) => {
        seen.push(`req ${i.endpoint} ${i.attempt}`);
        throw new Error("hook broke");
      },
      onResponse: async (i) => {
        seen.push(`res ${i.status}`);
        throw new Error("async hook broke");
      },
    });
    await expect(c.players.stats()).resolves.toBeDefined();
    expect(seen).toEqual(["req players.stats 0", "res 200"]);
  });

  it("keeps unknown response fields", async () => {
    const s = await startMock(() => ({ body: { success: true, accountId: 1, brandNewField: { x: 1 } } }));
    try {
      const me = await client(s).account.me();
      expect(me.brandNewField).toEqual({ x: 1 });
    } finally {
      await s.close();
    }
  });
});
