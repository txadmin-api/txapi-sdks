import { afterEach, describe, expect, it } from "vitest";
import { client, startMock, type MockServer, type Recorded } from "./helpers";

let server: MockServer | undefined;
afterEach(async () => {
  await server?.close();
  server = undefined;
});

function player(n: number) {
  return {
    license: `lic${n}`,
    displayName: `P${n}`,
    playTime: 1000 - n,
    tsJoined: 2000 - n,
    tsLastConnection: 3000 - n,
  };
}

/** Serves `total` players in pages of `size`, honouring offset/cursor by license. */
function playerPages(total: number, size: number) {
  const all = Array.from({ length: total }, (_, i) => player(i));
  return (req: Recorded) => {
    const cursor = req.query.get("cursor");
    const start = cursor ? all.findIndex((p) => p.license === cursor) + 1 : 0;
    const rows = all.slice(start, start + size);
    return { body: { players: rows, hasReachedEnd: start + size >= total } };
  };
}

describe("player pagination", () => {
  it("searchAll walks every page with offset = playTime, cursor = license", async () => {
    server = await startMock(playerPages(5, 2));
    const rows = await client(server).players.searchAll({ query: "x" }).toArray();
    expect(rows.map((r) => r.license)).toEqual(["lic0", "lic1", "lic2", "lic3", "lic4"]);
    expect(server.requests).toHaveLength(3);
    expect(server.requests[0]?.query.has("offset")).toBe(false);
    expect(server.requests[1]?.query.get("offset")).toBe("999");
    expect(server.requests[1]?.query.get("cursor")).toBe("lic1");
    expect(server.requests[2]?.query.get("offset")).toBe("997");
    expect(server.requests.every((r) => r.query.get("searchValue") === "x" || r.query.get("query") === "x")).toBe(true);
  });

  it.each([
    ["tsJoined", "tsJoined", 1999],
    ["sessions", "tsJoined", 1999],
    ["tsLastConnection", "tsLastConnection", 2999],
    ["lastConnection", "tsLastConnection", 2999],
  ] as const)("sortBy %s uses row field %s as offset", async (sortBy, _field, expected) => {
    server = await startMock(playerPages(3, 2));
    await client(server).players.searchAll({ sortBy }).toArray();
    expect(server.requests[0]?.query.get("sortBy")).toBe(sortBy);
    expect(server.requests[1]?.query.get("offset")).toBe(String(expected));
    expect(server.requests[1]?.query.get("cursor")).toBe("lic1");
  });

  it("onlineAll and lookupAll page by playTime", async () => {
    server = await startMock(playerPages(3, 2));
    await client(server).players.onlineAll().toArray();
    expect(server.requests[1]?.rawPath).toBe("/api/v1/players/online");
    expect(server.requests[1]?.query.get("offset")).toBe("999");
    server.requests.length = 0;
    await client(server).players.lookupAll("discord:1").toArray();
    expect(server.requests[1]?.query.get("identifier")).toBe("discord:1");
    expect(server.requests[1]?.query.get("cursor")).toBe("lic1");
  });

  it("pages() yields whole pages and toArray(limit) stops early", async () => {
    server = await startMock(playerPages(10, 3));
    const pages = [];
    for await (const page of client(server).players.searchAll().pages()) pages.push(page.players.length);
    expect(pages).toEqual([3, 3, 3, 1]);
    server.requests.length = 0;
    const first4 = await client(server).players.searchAll().toArray(4);
    expect(first4).toHaveLength(4);
    expect(server.requests).toHaveLength(2);
  });

  it("stops on an empty page even without hasReachedEnd", async () => {
    server = await startMock((_r, i) => ({ body: { players: i === 0 ? [player(0)] : [], hasReachedEnd: false } }));
    const rows = await client(server).players.searchAll().toArray();
    expect(rows).toHaveLength(1);
    expect(server.requests).toHaveLength(2);
  });

  it("stops when the cursor does not move", async () => {
    server = await startMock(() => ({ body: { players: [player(0)], hasReachedEnd: false } }));
    const rows = await client(server).players.searchAll().toArray();
    expect(rows).toHaveLength(2);
    expect(server.requests).toHaveLength(2);
  });
});

describe("history pagination", () => {
  it("bansAll uses offset = timestamp, cursor = id", async () => {
    const rows = [
      { id: "A1", type: "ban", timestamp: 300 },
      { id: "A2", type: "ban", timestamp: 200 },
      { id: "A3", type: "ban", timestamp: 100 },
    ];
    server = await startMock((req) => {
      const c = req.query.get("cursor");
      const start = c ? rows.findIndex((r) => r.id === c) + 1 : 0;
      return { body: { history: rows.slice(start, start + 2), hasReachedEnd: start + 2 >= rows.length } };
    });
    const all = await client(server).history.bansAll().toArray();
    expect(all.map((r) => r.id)).toEqual(["A1", "A2", "A3"]);
    expect(server.requests[1]?.rawPath).toBe("/api/v1/history/bans");
    expect(server.requests[1]?.query.get("offset")).toBe("200");
    expect(server.requests[1]?.query.get("cursor")).toBe("A2");
  });

  it("searchAll keeps the filters on every page", async () => {
    server = await startMock((_r, i) => ({
      body: { history: [{ id: `X${i}`, timestamp: 10 - i }], hasReachedEnd: i === 1 },
    }));
    await client(server).history.searchAll({ type: "ban", search: "cheat", searchType: "reason" }).toArray();
    expect(server.requests).toHaveLength(2);
    for (const r of server.requests) {
      expect(r.query.get("type")).toBe("ban");
      expect(r.query.get("search")).toBe("cheat");
    }
    expect(server.requests[1]?.query.get("cursor")).toBe("X0");
  });
});

describe("event pagination", () => {
  it("listAll follows nextCursor until it is absent", async () => {
    server = await startMock((_r, i) => ({
      body: {
        eventLog: { enabled: true },
        events: [{ id: i, type: "join" }],
        ...(i < 2 ? { nextCursor: `c${i}` } : {}),
      },
    }));
    const events = await client(server).events.listAll({ type: "join" }).toArray();
    expect(events.map((e) => e.id)).toEqual([0, 1, 2]);
    expect(server.requests[0]?.query.has("cursor")).toBe(false);
    expect(server.requests[1]?.query.get("cursor")).toBe("c0");
    expect(server.requests[2]?.query.get("cursor")).toBe("c1");
    expect(server.requests.every((r) => r.query.get("type") === "join")).toBe(true);
  });

  it("killsAll, chatAll and sessionsAll page on their own paths", async () => {
    server = await startMock((_r, i) => ({ body: { events: [{ id: i }], ...(i % 2 === 0 ? { nextCursor: "n" } : {}) } }));
    const c = client(server);
    await c.events.killsAll().toArray();
    await c.events.chatAll().toArray();
    await c.events.sessionsAll().toArray();
    expect(server.requests.map((r) => r.rawPath)).toEqual([
      "/api/v1/events/kills",
      "/api/v1/events/kills",
      "/api/v1/events/chat",
      "/api/v1/events/chat",
      "/api/v1/events/sessions",
      "/api/v1/events/sessions",
    ]);
  });
});
