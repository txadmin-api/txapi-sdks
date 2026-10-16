import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import type { AddressInfo } from "node:net";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { TxApi, type TxApiOptions } from "../src";

export interface Recorded {
  method: string;
  /** Raw (still percent-encoded) path without the query string. */
  rawPath: string;
  query: URLSearchParams;
  headers: IncomingMessage["headers"];
  bodyText: string;
  body: unknown;
}

export interface MockReply {
  status?: number;
  headers?: Record<string, string>;
  body?: unknown;
  /** Send `body` verbatim (no JSON encoding). */
  raw?: boolean;
  /** Destroy the socket without answering (simulates a connection error). */
  destroy?: boolean;
  /** Wait before answering. */
  delayMs?: number;
}

export type MockHandler = (req: Recorded, index: number) => MockReply | Promise<MockReply>;

export interface MockServer {
  url: string;
  requests: Recorded[];
  close(): Promise<void>;
}

export const RATE_HEADERS = {
  "X-RateLimit-Limit": "120",
  "X-RateLimit-Remaining": "119",
  "X-RateLimit-Reset": "42",
};

export async function startMock(handler: MockHandler = () => ({ body: { success: true, message: "ok" } })): Promise<MockServer> {
  const requests: Recorded[] = [];
  const server = createServer((req: IncomingMessage, res: ServerResponse) => {
    const chunks: Buffer[] = [];
    req.on("data", (c: Buffer) => chunks.push(c));
    req.on("end", async () => {
      const bodyText = Buffer.concat(chunks).toString("utf8");
      let body: unknown;
      try {
        body = bodyText ? JSON.parse(bodyText) : undefined;
      } catch {
        body = bodyText;
      }
      const raw = req.url ?? "/";
      const q = raw.indexOf("?");
      const rec: Recorded = {
        method: req.method ?? "GET",
        rawPath: q < 0 ? raw : raw.slice(0, q),
        query: new URLSearchParams(q < 0 ? "" : raw.slice(q + 1)),
        headers: req.headers,
        bodyText,
        body,
      };
      requests.push(rec);
      const reply = await handler(rec, requests.length - 1);
      if (reply.delayMs) await new Promise((r) => setTimeout(r, reply.delayMs));
      if (reply.destroy) {
        req.socket.destroy();
        return;
      }
      const status = reply.status ?? 200;
      const headers: Record<string, string> = { ...RATE_HEADERS, ...(reply.headers ?? {}) };
      let payload = "";
      if (reply.body !== undefined) {
        if (reply.raw) payload = String(reply.body);
        else {
          payload = JSON.stringify(reply.body);
          headers["Content-Type"] ??= "application/json";
        }
      }
      res.writeHead(status, headers);
      res.end(payload);
    });
  });
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  const { port } = server.address() as AddressInfo;
  return {
    url: `http://127.0.0.1:${port}`,
    requests,
    close: () =>
      new Promise<void>((resolve) => {
        server.closeAllConnections();
        server.close(() => resolve());
      }),
  };
}

export function client(server: MockServer, options: TxApiOptions = {}): TxApi {
  return new TxApi({ apiKey: "txp_test_SECRETKEY", baseUrl: server.url, serverId: 1, maxRetries: 0, ...options });
}

export interface ManifestEndpoint {
  id: string;
  method: string;
  path: string;
  permission: string | null;
  accountLevel?: boolean;
  params?: Array<{ name: string; in: string; required: boolean; type: string }>;
  body?: Array<{ name: string; type: string; required: boolean; maxLength?: number }>;
}

export interface Manifest {
  apiVersion: string;
  endpoints: ManifestEndpoint[];
  websockets: Array<{ id: string; path: string; permission: string }>;
  webhookEvents: string[];
}

export function loadManifest(): Manifest {
  const path = fileURLToPath(new URL("../../spec/endpoints.json", import.meta.url));
  return JSON.parse(readFileSync(path, "utf8")) as Manifest;
}

export function loadVectors(): {
  secret: string;
  nowSeconds: number;
  toleranceSeconds: number;
  body: string;
  valid: { header: string };
  validWithRotatedSecret: { header: string };
  invalid: Array<{ name: string; header: string; body?: string }>;
} {
  const path = fileURLToPath(new URL("../../spec/webhook-vectors.json", import.meta.url));
  return JSON.parse(readFileSync(path, "utf8"));
}

/** Maps a concrete raw path back onto a manifest template (literal templates win). */
export function templateFor(manifest: Manifest, method: string, rawPath: string): string | undefined {
  const candidates = manifest.endpoints.filter((e) => e.method === method);
  const literal = candidates.find((e) => !e.path.includes("{") && e.path === rawPath);
  if (literal) return `${method} ${literal.path}`;
  for (const e of candidates) {
    if (!e.path.includes("{")) continue;
    const re = new RegExp(`^${e.path.replace(/\{\w+\}/g, "[^/]+")}$`);
    if (re.test(rawPath)) return `${method} ${e.path}`;
  }
  return undefined;
}
