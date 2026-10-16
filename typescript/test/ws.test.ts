import { afterEach, describe, expect, it } from "vitest";
import { WebSocketServer, WebSocket as NodeWS, type WebSocket as ServerSocket } from "ws";
import type { AddressInfo } from "node:net";
import {
  CommandRejectedError,
  ConnectionError,
  StreamClosedError,
  TimeoutError,
  TxApi,
  ValidationError,
  type ConsoleStreamItem,
  type WebSocketConstructor,
} from "../src";

interface WsMock {
  url: string;
  connections: Array<{ url: string; socket: ServerSocket }>;
  received: unknown[];
  close(): Promise<void>;
}

type OnConnect = (socket: ServerSocket, index: number, url: string) => void;

async function startWs(onConnect: OnConnect): Promise<WsMock> {
  const wss = new WebSocketServer({ port: 0, host: "127.0.0.1" });
  await new Promise<void>((r) => wss.once("listening", () => r()));
  const mock: WsMock = {
    url: `http://127.0.0.1:${(wss.address() as AddressInfo).port}`,
    connections: [],
    received: [],
    close: () =>
      new Promise<void>((resolve) => {
        for (const c of wss.clients) c.terminate();
        wss.close(() => resolve());
      }),
  };
  wss.on("connection", (socket, req) => {
    const url = req.url ?? "";
    mock.connections.push({ url, socket });
    socket.on("message", (data) => mock.received.push(JSON.parse(String(data))));
    onConnect(socket, mock.connections.length - 1, url);
  });
  return mock;
}

/** A console mock: init on connect, ack/error per command, "slow" is never answered. */
function consoleBehaviour(socket: ServerSocket, index: number): void {
  socket.send(JSON.stringify({ type: "init", lines: [`boot ${index}`, "ready"] }));
  socket.on("message", (raw) => {
    const frame = JSON.parse(String(raw)) as { type: string; command: string };
    if (frame.command === "slow") return;
    if (frame.command === "fail") {
      socket.send(JSON.stringify({ type: "error", message: "txAdmin refused" }));
      return;
    }
    socket.send(JSON.stringify({ type: "ack", message: "Command sent" }));
    socket.send(JSON.stringify({ type: "log", lines: [`> ${frame.command}`] }));
  });
}

const fast = { initialDelayMs: 20, maxDelayMs: 50 };

let mock: WsMock | undefined;
let api: TxApi | undefined;
const streams: Array<{ close(): Promise<unknown> }> = [];
afterEach(async () => {
  for (const s of streams.splice(0)) await s.close();
  await mock?.close();
  mock = undefined;
});

function makeClient(m: WsMock): TxApi {
  api = new TxApi({
    apiKey: "txp_test_SECRETKEY",
    baseUrl: m.url,
    serverId: 7,
    WebSocket: NodeWS as unknown as WebSocketConstructor,
  });
  return api;
}

function waitFor<T>(fn: () => T | undefined, timeoutMs = 3000): Promise<T> {
  const started = Date.now();
  return new Promise((resolve, reject) => {
    const tick = () => {
      const v = fn();
      if (v !== undefined && v !== false) return resolve(v);
      if (Date.now() - started > timeoutMs) return reject(new Error("waitFor timed out"));
      setTimeout(tick, 10);
    };
    tick();
  });
}

describe("console stream", () => {
  it("connects with apiKey + serverId in the query and delivers init/log frames", async () => {
    mock = await startWs(consoleBehaviour);
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    const inits: string[][] = [];
    const logs: string[] = [];
    live.on("init", (lines) => inits.push(lines));
    live.on("log", (line) => logs.push(line));
    await live.ready();
    expect(live.connected).toBe(true);

    const url = new URL(mock.connections[0]!.url, "http://x");
    expect(url.pathname).toBe("/ws/console");
    expect(url.searchParams.get("apiKey")).toBe("txp_test_SECRETKEY");
    expect(url.searchParams.get("serverId")).toBe("7");

    await expect(live.sendCommand("status")).resolves.toBe("Command sent");
    expect(mock.received).toEqual([{ type: "command", command: "status" }]);
    await waitFor(() => (logs.length > 0 ? true : undefined));
    expect(inits).toEqual([["boot 0", "ready"]]);
    expect(logs).toEqual(["> status"]);
  });

  it("is async-iterable (init lines first, then log lines)", async () => {
    mock = await startWs(consoleBehaviour);
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    const items: ConsoleStreamItem[] = [];
    const reader = (async () => {
      for await (const item of live) {
        items.push(item);
        if (items.length === 3) break;
      }
    })();
    await live.ready();
    await live.sendCommand("hello");
    await reader;
    expect(items).toEqual([
      { type: "init", line: "boot 0" },
      { type: "init", line: "ready" },
      { type: "log", line: "> hello" },
    ]);
  });

  it("sendCommand rejects with CommandRejectedError on an error frame", async () => {
    mock = await startWs(consoleBehaviour);
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    await live.ready();
    const err = await live.sendCommand("fail").catch((e: unknown) => e);
    expect(err).toBeInstanceOf(CommandRejectedError);
    expect((err as Error).message).toBe("txAdmin refused");
    // The stream stays usable.
    await expect(live.sendCommand("next")).resolves.toBe("Command sent");
  });

  it("acks are matched FIFO", async () => {
    mock = await startWs(consoleBehaviour);
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    await live.ready();
    const results = await Promise.allSettled([live.sendCommand("a"), live.sendCommand("fail"), live.sendCommand("c")]);
    expect(results.map((r) => r.status)).toEqual(["fulfilled", "rejected", "fulfilled"]);
  });

  it("sendCommand times out with TimeoutError", async () => {
    mock = await startWs(consoleBehaviour);
    const live = makeClient(mock).console.connect({ reconnect: fast, commandTimeout: 100 });
    streams.push(live);
    await live.ready();
    const err = await live.sendCommand("slow").catch((e: unknown) => e);
    expect(err).toBeInstanceOf(TimeoutError);
  });

  it("validates commands before sending and refuses when not connected", async () => {
    mock = await startWs(consoleBehaviour);
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    await expect(live.sendCommand("early")).rejects.toBeInstanceOf(ConnectionError);
    await live.ready();
    await expect(live.sendCommand("   ")).rejects.toBeInstanceOf(ValidationError);
    await expect(live.sendCommand("x".repeat(1025))).rejects.toBeInstanceOf(ValidationError);
    expect(mock.received).toEqual([]);
  });

  it("reconnects after the server closes with 1011 and receives a fresh init", async () => {
    mock = await startWs((socket, index) => {
      consoleBehaviour(socket, index);
      if (index === 0) setTimeout(() => socket.close(1011, "txAdmin connection failed"), 30);
    });
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    const inits: string[][] = [];
    const reconnects: number[] = [];
    const closes: boolean[] = [];
    live.on("init", (lines) => inits.push(lines));
    live.on("reconnecting", (info) => reconnects.push(info.code));
    live.on("close", (info) => closes.push(info.willReconnect));
    await waitFor(() => (inits.length >= 2 ? true : undefined));
    expect(inits).toEqual([
      ["boot 0", "ready"],
      ["boot 1", "ready"],
    ]);
    expect(reconnects).toEqual([1011]);
    expect(closes).toEqual([true]);
    expect(mock.connections).toHaveLength(2);
    await waitFor(() => (live.connected ? true : undefined));
    await expect(live.sendCommand("after")).resolves.toBe("Command sent");
  });

  it("also reconnects after an abrupt connection loss (1006)", async () => {
    mock = await startWs((socket, index) => {
      consoleBehaviour(socket, index);
      if (index === 0) setTimeout(() => socket.terminate(), 30);
    });
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    await waitFor(() => (mock!.connections.length >= 2 ? true : undefined));
  });

  it("does not reconnect on 1008 and surfaces the reason", async () => {
    mock = await startWs((socket) => socket.close(1008, "Monthly quota exceeded"));
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    const ready = await live.ready().catch((e: unknown) => e);
    expect(ready).toBeInstanceOf(StreamClosedError);
    expect((ready as StreamClosedError).code).toBe(1008);
    expect((ready as StreamClosedError).reason).toBe("Monthly quota exceeded");
    expect((ready as Error).message).not.toContain("SECRETKEY");
    const info = await live.closed();
    expect(info).toEqual({ code: 1008, reason: "Monthly quota exceeded", willReconnect: false });
    expect(live.finished).toBe(true);
    await new Promise((r) => setTimeout(r, 150));
    expect(mock.connections).toHaveLength(1);
  });

  it("a 1008 after being connected ends the iterator with StreamClosedError", async () => {
    mock = await startWs((socket, index) => {
      consoleBehaviour(socket, index);
      setTimeout(() => socket.close(1008, "API key revoked"), 50);
    });
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    const err = await (async () => {
      for await (const _item of live) {
        /* drain */
      }
    })().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(StreamClosedError);
    expect((err as StreamClosedError).reason).toBe("API key revoked");
  });

  it("pending commands reject with ConnectionError when the connection drops", async () => {
    mock = await startWs((socket, index) => {
      consoleBehaviour(socket, index);
      socket.on("message", () => setTimeout(() => socket.close(1011, "gone"), 10));
    });
    const live = makeClient(mock).console.connect({ reconnect: fast });
    streams.push(live);
    await live.ready();
    await expect(live.sendCommand("slow")).rejects.toBeInstanceOf(ConnectionError);
  });

  it("close() stops the stream: no reconnect, iterator ends cleanly", async () => {
    mock = await startWs(consoleBehaviour);
    const live = makeClient(mock).console.connect({ reconnect: fast });
    const reader = (async () => {
      const out: ConsoleStreamItem[] = [];
      for await (const item of live) out.push(item);
      return out;
    })();
    await live.ready();
    await new Promise((r) => setTimeout(r, 30));
    const info = await live.close();
    expect(info.willReconnect).toBe(false);
    const items = await reader;
    expect(items.length).toBeGreaterThanOrEqual(2);
    await new Promise((r) => setTimeout(r, 150));
    expect(mock.connections).toHaveLength(1);
    expect(live.connected).toBe(false);
  });

  it("reconnect: false finishes on the first drop", async () => {
    mock = await startWs((socket, index) => {
      consoleBehaviour(socket, index);
      setTimeout(() => socket.close(1012, "restarting"), 30);
    });
    const live = makeClient(mock).console.connect({ reconnect: false });
    streams.push(live);
    const info = await live.closed();
    expect(info.code).toBe(1012);
    expect(mock.connections).toHaveLength(1);
  });

  it("works with the global WebSocket (Node 22+)", async () => {
    if (typeof (globalThis as { WebSocket?: unknown }).WebSocket !== "function") return;
    mock = await startWs(consoleBehaviour);
    const live = new TxApi({ apiKey: "txp_test_SECRETKEY", baseUrl: mock.url, serverId: 7 }).console.connect({
      reconnect: fast,
    });
    streams.push(live);
    await live.ready();
    await expect(live.sendCommand("status")).resolves.toBe("Command sent");
  });
});

describe("server log stream", () => {
  it("delivers init/log entries and stays read-only", async () => {
    mock = await startWs((socket) => {
      socket.send(JSON.stringify({ type: "init", entries: [{ type: "playerJoining", msg: "joined" }, "plain line"] }));
      setTimeout(() => socket.send(JSON.stringify({ type: "log", entries: [{ type: "playerDropped", msg: "left" }] })), 20);
    });
    const log = makeClient(mock).serverlog.connect({ serverId: 9, reconnect: fast });
    streams.push(log);
    const inits: unknown[] = [];
    const logs: unknown[] = [];
    log.on("init", (entries) => inits.push(entries));
    log.on("log", (entry) => logs.push(entry));
    await log.ready();
    await waitFor(() => (logs.length > 0 ? true : undefined));
    expect(inits).toEqual([[{ type: "playerJoining", msg: "joined" }, "plain line"]]);
    expect(logs).toEqual([{ type: "playerDropped", msg: "left" }]);
    const url = new URL(mock.connections[0]!.url, "http://x");
    expect(url.pathname).toBe("/ws/serverlog");
    expect(url.searchParams.get("serverId")).toBe("9");
  });

  it("an error frame is emitted as an error event", async () => {
    mock = await startWs((socket) => {
      socket.send(JSON.stringify({ type: "error", message: "txAdmin unreachable" }));
    });
    const log = makeClient(mock).serverlog.connect({ reconnect: fast });
    streams.push(log);
    const err = await new Promise<Error>((resolve) => log.on("error", resolve));
    expect(err.message).toBe("txAdmin unreachable");
  });
});

describe("missing WebSocket implementation", () => {
  it("explains how to inject `ws`", () => {
    const original = (globalThis as { WebSocket?: unknown }).WebSocket;
    (globalThis as { WebSocket?: unknown }).WebSocket = undefined;
    try {
      const c = new TxApi({ apiKey: "txp_test_SECRETKEY", baseUrl: "http://127.0.0.1:1", serverId: 1 });
      expect(() => c.console.connect()).toThrow(/ws/);
    } finally {
      (globalThis as { WebSocket?: unknown }).WebSocket = original;
    }
  });
});
