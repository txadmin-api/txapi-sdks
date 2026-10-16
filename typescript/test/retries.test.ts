import { afterEach, describe, expect, it } from "vitest";
import { ConnectionError, RateLimitError, TimeoutError, TxAdminUnavailableError, ServerError } from "../src";
import { client, startMock, type MockServer } from "./helpers";

let server: MockServer | undefined;
afterEach(async () => {
  await server?.close();
  server = undefined;
});

const ok = { body: { success: true, message: "ok" } };

describe("retries", () => {
  it("429 with Retry-After is retried for GET", async () => {
    server = await startMock((_r, i) => (i === 0 ? { status: 429, headers: { "Retry-After": "0" }, body: { error: "slow" } } : ok));
    const res = await client(server, { maxRetries: 2 }).server.status();
    expect(res).toEqual({ success: true, message: "ok" });
    expect(server.requests).toHaveLength(2);
  });

  it("429 with Retry-After is retried for POST too (request was not executed)", async () => {
    server = await startMock((_r, i) => (i === 0 ? { status: 429, headers: { "Retry-After": "1" }, body: { error: "slow" } } : ok));
    const started = Date.now();
    await client(server, { maxRetries: 2 }).players.ban({ identifier: "license:abc", reason: "x" });
    expect(Date.now() - started).toBeGreaterThanOrEqual(900);
    expect(server.requests.map((r) => r.method)).toEqual(["POST", "POST"]);
    expect(server.requests[1]?.body).toEqual({ identifier: "license:abc", reason: "x" });
  });

  it("429 without Retry-After (quota) is never retried", async () => {
    server = await startMock(() => ({ status: 429, body: { error: "quota" } }));
    const err = await client(server, { maxRetries: 3 }).server.status().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(RateLimitError);
    expect((err as RateLimitError).quotaExhausted).toBe(true);
    expect(server.requests).toHaveLength(1);
  });

  it("429 with Retry-After above 60 s is not retried", async () => {
    server = await startMock(() => ({ status: 429, headers: { "Retry-After": "120" }, body: { error: "slow" } }));
    const err = await client(server, { maxRetries: 3 }).server.status().catch((e: unknown) => e);
    expect((err as RateLimitError).retryAfter).toBe(120);
    expect(server.requests).toHaveLength(1);
  });

  it("502 on GET is retried until success", async () => {
    server = await startMock((_r, i) => (i < 2 ? { status: 502, body: { error: "txAdmin down" } } : ok));
    const res = await client(server, { maxRetries: 2 }).server.status();
    expect(res).toEqual({ success: true, message: "ok" });
    expect(server.requests).toHaveLength(3);
  });

  it("502 on GET gives up after maxRetries", async () => {
    server = await startMock(() => ({ status: 502, body: { error: "txAdmin down" } }));
    const err = await client(server, { maxRetries: 2 }).server.status().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(TxAdminUnavailableError);
    expect(server.requests).toHaveLength(3);
  });

  it("503 and 504 on GET are retried, 500 is not", async () => {
    server = await startMock((_r, i) => (i === 0 ? { status: 503 } : i === 1 ? { status: 504 } : ok));
    await client(server, { maxRetries: 2 }).server.status();
    expect(server.requests).toHaveLength(3);
    await server.close();

    server = await startMock(() => ({ status: 500, body: { error: "bug" } }));
    const err = await client(server, { maxRetries: 2 }).server.status().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ServerError);
    expect(server.requests).toHaveLength(1);
  });

  it("502 on POST is not retried", async () => {
    server = await startMock(() => ({ status: 502, body: { error: "txAdmin down" } }));
    const err = await client(server, { maxRetries: 3 }).players.kick({ identifier: "license:abc", reason: "x" }).catch((e: unknown) => e);
    expect(err).toBeInstanceOf(TxAdminUnavailableError);
    expect(server.requests).toHaveLength(1);
  });

  it("connection errors are retried for GET only", async () => {
    server = await startMock((_r, i) => (i === 0 ? { destroy: true } : ok));
    await client(server, { maxRetries: 1 }).server.status();
    expect(server.requests).toHaveLength(2);
    await server.close();

    server = await startMock(() => ({ destroy: true }));
    const err = await client(server, { maxRetries: 3 }).server.restart().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ConnectionError);
    expect(server.requests).toHaveLength(1);
  });

  it("timeouts are retried for GET", async () => {
    server = await startMock((_r, i) => (i === 0 ? { delayMs: 300, ...ok } : ok));
    await client(server, { maxRetries: 1, timeout: 50 }).server.status();
    expect(server.requests).toHaveLength(2);
  });

  it("maxRetries: 0 disables retries", async () => {
    server = await startMock(() => ({ status: 502, body: { error: "down" } }));
    await client(server, { maxRetries: 0 }).server.status().catch(() => undefined);
    expect(server.requests).toHaveLength(1);
  });

  it("per-request maxRetries overrides the client option", async () => {
    server = await startMock(() => ({ status: 502, body: { error: "down" } }));
    await client(server, { maxRetries: 3 }).server.status({ maxRetries: 0 }).catch(() => undefined);
    expect(server.requests).toHaveLength(1);
  });

  it("onRequest sees increasing attempt numbers", async () => {
    server = await startMock((_r, i) => (i === 0 ? { status: 503 } : ok));
    const attempts: number[] = [];
    await client(server, { maxRetries: 2, onRequest: (info) => attempts.push(info.attempt) }).server.status();
    expect(attempts).toEqual([0, 1]);
  });
});

describe("abort", () => {
  it("an aborted signal rejects with an AbortError and sends nothing more", async () => {
    server = await startMock(() => ({ delayMs: 500, ...ok }));
    const controller = new AbortController();
    const p = client(server).server.status({ signal: controller.signal });
    setTimeout(() => controller.abort(), 30);
    const err = await p.catch((e: unknown) => e);
    expect((err as Error).name).toBe("AbortError");
    expect(err).not.toBeInstanceOf(TimeoutError);
  });

  it("abort during a retry wait stops the retry", async () => {
    server = await startMock(() => ({ status: 429, headers: { "Retry-After": "5" }, body: { error: "slow" } }));
    const controller = new AbortController();
    const started = Date.now();
    const p = client(server, { maxRetries: 3 }).server.status({ signal: controller.signal });
    setTimeout(() => controller.abort(), 100);
    const err = await p.catch((e: unknown) => e);
    expect((err as Error).name).toBe("AbortError");
    expect(Date.now() - started).toBeLessThan(2000);
    expect(server.requests).toHaveLength(1);
  });

  it("an already-aborted signal never hits the network", async () => {
    server = await startMock(() => ok);
    const controller = new AbortController();
    controller.abort();
    const err = await client(server).server.status({ signal: controller.signal }).catch((e: unknown) => e);
    expect((err as Error).name).toBe("AbortError");
    expect(server.requests).toHaveLength(0);
  });
});
