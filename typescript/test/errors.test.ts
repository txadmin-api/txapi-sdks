import { afterEach, describe, expect, it } from "vitest";
import {
  AuthenticationError,
  BadRequestError,
  ConflictError,
  ConnectionError,
  NotFoundError,
  PermissionDeniedError,
  RateLimitError,
  ServerError,
  TimeoutError,
  TxAdminRejectedError,
  TxAdminUnavailableError,
  TxApiError,
  parseRateLimit,
  parseRetryAfter,
} from "../src";
import { client, startMock, type MockServer } from "./helpers";

let server: MockServer | undefined;
afterEach(async () => {
  await server?.close();
  server = undefined;
});

const CASES: Array<[number, new (...args: never[]) => TxApiError]> = [
  [400, BadRequestError],
  [401, AuthenticationError],
  [403, PermissionDeniedError],
  [404, NotFoundError],
  [405, BadRequestError],
  [409, ConflictError],
  [415, BadRequestError],
  [422, TxAdminRejectedError],
  [429, RateLimitError],
  [500, ServerError],
  [502, TxAdminUnavailableError],
  [503, ServerError],
  [504, ServerError],
];

describe("error mapping", () => {
  for (const [status, Cls] of CASES) {
    it(`${status} → ${Cls.name}`, async () => {
      server = await startMock(() => ({ status, body: { success: false, error: `boom ${status}` } }));
      const err = await client(server).server.status().catch((e: unknown) => e);
      expect(err).toBeInstanceOf(Cls);
      expect(err).toBeInstanceOf(TxApiError);
      const e = err as TxApiError;
      expect(e.status).toBe(status);
      expect(e.message).toBe(`boom ${status}`);
      expect(e.name).toBe(Cls.name);
      expect(e.body).toEqual({ success: false, error: `boom ${status}` });
      expect(e.headers?.get("x-ratelimit-limit")).toBe("120");
      expect(e.rateLimit).toEqual({ limit: 120, remaining: 119, resetSeconds: 42 });
      expect(e.message).not.toContain("SECRETKEY");
    });
  }

  it("unlisted 4xx → base TxApiError", async () => {
    server = await startMock(() => ({ status: 418, body: { error: "teapot" } }));
    const err = await client(server).account.me().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(TxApiError);
    expect(err).not.toBeInstanceOf(BadRequestError);
    expect((err as TxApiError).status).toBe(418);
  });

  it("429 with Retry-After: retryAfter set, not quotaExhausted", async () => {
    server = await startMock(() => ({ status: 429, headers: { "Retry-After": "120" }, body: { error: "slow down" } }));
    const err = (await client(server).server.status().catch((e: unknown) => e)) as RateLimitError;
    expect(err).toBeInstanceOf(RateLimitError);
    expect(err.retryAfter).toBe(120);
    expect(err.quotaExhausted).toBe(false);
  });

  it("429 without Retry-After: quotaExhausted", async () => {
    server = await startMock(() => ({ status: 429, body: { error: "Monthly quota exceeded" } }));
    const err = (await client(server).server.status().catch((e: unknown) => e)) as RateLimitError;
    expect(err.retryAfter).toBeNull();
    expect(err.quotaExhausted).toBe(true);
    expect(err.message).toBe("Monthly quota exceeded");
  });

  it("plain-text body becomes the message", async () => {
    server = await startMock(() => ({ status: 502, raw: true, body: "Bad Gateway from proxy", headers: { "Content-Type": "text/plain" } }));
    const err = (await client(server).server.status().catch((e: unknown) => e)) as TxApiError;
    expect(err).toBeInstanceOf(TxAdminUnavailableError);
    expect(err.message).toBe("Bad Gateway from proxy");
    expect(err.body).toBe("Bad Gateway from proxy");
  });

  it("HTML error page is not parsed as JSON", async () => {
    server = await startMock(() => ({ status: 503, raw: true, body: "<html>down</html>", headers: { "Content-Type": "text/html" } }));
    const err = (await client(server).server.status().catch((e: unknown) => e)) as TxApiError;
    expect(err).toBeInstanceOf(ServerError);
    expect(err.message).toBe("<html>down</html>");
  });

  it("empty body falls back to the status text", async () => {
    server = await startMock(() => ({ status: 404 }));
    const err = (await client(server).server.status().catch((e: unknown) => e)) as TxApiError;
    expect(err).toBeInstanceOf(NotFoundError);
    expect(err.message).toMatch(/Not Found|HTTP 404/);
  });

  it("`message` used when there is no `error`", async () => {
    server = await startMock(() => ({ status: 400, body: { message: "bad thing" } }));
    const err = (await client(server).server.status().catch((e: unknown) => e)) as TxApiError;
    expect(err.message).toBe("bad thing");
  });

  it("connection error → ConnectionError", async () => {
    server = await startMock(() => ({ destroy: true }));
    const err = await client(server).server.status().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ConnectionError);
    expect((err as Error).message).not.toContain("SECRETKEY");
  });

  it("timeout → TimeoutError", async () => {
    server = await startMock(() => ({ delayMs: 500, body: {} }));
    const err = await client(server, { timeout: 50 }).server.status().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(TimeoutError);
  });
});

describe("rate-limit headers", () => {
  it("parses the three headers and exposes lastRateLimit", async () => {
    server = await startMock(() => ({ body: { online: true } }));
    const c = client(server);
    expect(c.lastRateLimit).toBeNull();
    const { response: { rateLimit } } = await c.server.status().withResponse();
    expect(rateLimit).toEqual({ limit: 120, remaining: 119, resetSeconds: 42 });
    expect(c.lastRateLimit).toEqual({ limit: 120, remaining: 119, resetSeconds: 42 });
  });

  it("missing headers → null; partial → null fields", () => {
    expect(parseRateLimit(new Headers())).toBeNull();
    expect(parseRateLimit(new Headers({ "X-RateLimit-Remaining": "5" }))).toEqual({
      limit: null,
      remaining: 5,
      resetSeconds: null,
    });
    expect(parseRateLimit(new Headers({ "X-RateLimit-Limit": "abc", "X-RateLimit-Remaining": "1" }))).toEqual({
      limit: null,
      remaining: 1,
      resetSeconds: null,
    });
  });

  it("Retry-After accepts seconds and an HTTP date", () => {
    expect(parseRetryAfter(new Headers({ "Retry-After": "7" }), 0)).toBe(7);
    expect(parseRetryAfter(new Headers(), 0)).toBeNull();
    const now = Date.UTC(2026, 0, 1, 0, 0, 0);
    const later = new Date(now + 30_000).toUTCString();
    expect(parseRetryAfter(new Headers({ "Retry-After": later }), now)).toBe(30);
    expect(parseRetryAfter(new Headers({ "Retry-After": "garbage" }), now)).toBeNull();
  });
});
