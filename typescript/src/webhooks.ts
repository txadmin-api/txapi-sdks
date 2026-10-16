import { WebhookVerificationError } from "./core/errors";

/** Every webhook event name the API can send (`spec/endpoints.json → webhookEvents`). */
export const WEBHOOK_EVENTS = [
  "action.revoked",
  "console.command",
  "log.admin_action",
  "log.ban",
  "log.kill",
  "log.warn",
  "ping",
  "player.ban_synced",
  "player.banned",
  "player.kicked",
  "player.messaged",
  "player.note",
  "player.warned",
  "player.whitelist",
  "quota.warning",
  "schedule.ran",
  "server.announcement",
  "server.kickall",
  "server.offline",
  "server.online",
  "server.resource",
  "server.restart_scheduled",
  "server.restart_skipped",
  "server.restarted",
  "server.started",
  "server.stopped",
  "txadmin.admin",
  "whitelist.approval_added",
  "whitelist.approval_removed",
  "whitelist.approved",
  "whitelist.denied",
  "whitelist.denied_all",
] as const;

/** Name of a webhook event. */
export type WebhookEvent = (typeof WEBHOOK_EVENTS)[number];

/** Request header carrying the signature: `t=<unix seconds>,v1=<hex>[,v1=<hex>]`. */
export const SIGNATURE_HEADER = "X-Txapi-Signature";
/** Request header carrying the event name. */
export const EVENT_HEADER = "X-Txapi-Event";
/** Request header carrying the delivery id (stable across retries). */
export const DELIVERY_HEADER = "X-Txapi-Delivery";

/** A verified webhook delivery. */
export interface WebhookPayload<E extends string = WebhookEvent | (string & {})> {
  /** Stable across retries — use it to skip duplicates. */
  id: string;
  event: E;
  /** ISO-8601. */
  createdAt: string;
  /** Absent/`null` for account-level events (e.g. `quota.warning`). */
  serverId?: number | null;
  /** Event-specific data; for API-triggered events `data.request` holds the (masked) request. */
  data: Record<string, unknown>;
  [key: string]: unknown;
}

export interface VerifyWebhookOptions {
  /** Max. clock difference in seconds, default 300. */
  toleranceSeconds?: number;
  /** Current time as Unix seconds or `Date` (for tests). Default: now. */
  now?: number | Date;
}

const encoder = new TextEncoder();

async function subtle(): Promise<SubtleCrypto> {
  const globalCrypto = (globalThis as { crypto?: Crypto }).crypto;
  if (globalCrypto?.subtle) return globalCrypto.subtle;
  // Node.js 18 without the global: fall back to node:crypto's webcrypto.
  const specifier = "node:crypto";
  const mod = (await import(/* @vite-ignore */ /* webpackIgnore: true */ specifier)) as {
    webcrypto?: { subtle?: SubtleCrypto };
  };
  if (mod.webcrypto?.subtle) return mod.webcrypto.subtle;
  throw new WebhookVerificationError("Web Crypto (crypto.subtle) is not available in this runtime");
}

function toHex(buffer: ArrayBuffer): string {
  let out = "";
  for (const byte of new Uint8Array(buffer)) out += byte.toString(16).padStart(2, "0");
  return out;
}

/** Constant-time comparison of two strings (time depends only on the length). */
export function timingSafeEqual(a: string, b: string): boolean {
  const ab = encoder.encode(a);
  const bb = encoder.encode(b);
  let diff = ab.length ^ bb.length;
  const length = Math.max(ab.length, bb.length);
  for (let i = 0; i < length; i++) diff |= (ab[i] ?? 0) ^ (bb[i] ?? 0);
  return diff === 0;
}

interface ParsedHeader {
  timestamp: number;
  signatures: string[];
}

function parseHeader(header: string | null | undefined): ParsedHeader {
  if (!header || header.trim() === "") throw new WebhookVerificationError("Missing signature header");
  let timestamp: number | undefined;
  const signatures: string[] = [];
  for (const part of header.split(",")) {
    const index = part.indexOf("=");
    if (index < 0) continue;
    const key = part.slice(0, index).trim();
    const value = part.slice(index + 1).trim();
    if (key === "t") {
      if (!/^\d{1,12}$/.test(value)) throw new WebhookVerificationError("Invalid timestamp in signature header");
      timestamp = Number(value);
    } else if (key === "v1") {
      signatures.push(value.toLowerCase());
    }
  }
  if (timestamp === undefined) throw new WebhookVerificationError("Missing timestamp in signature header");
  if (signatures.length === 0) throw new WebhookVerificationError("Missing v1 signature in signature header");
  return { timestamp, signatures };
}

/**
 * Verifies a webhook delivery and returns the parsed event.
 *
 * **Pass the raw request body** (string or bytes exactly as received). Parsing the JSON and
 * serialising it again changes the bytes and breaks the signature.
 *
 * Signature: `X-Txapi-Signature: t=<unix seconds>,v1=<hex>` where
 * `hex = HMAC-SHA256(secret, "<t>.<rawBody>")`. Several `v1` entries are accepted
 * (valid if any matches). Rejects timestamps more than `toleranceSeconds` (default 300)
 * away from now.
 *
 * @throws {@link WebhookVerificationError} with the reason.
 */
export async function verifyWebhook<E extends string = WebhookEvent | (string & {})>(
  rawBody: string | Uint8Array | ArrayBuffer,
  signatureHeader: string | null | undefined,
  secret: string,
  options: VerifyWebhookOptions = {},
): Promise<WebhookPayload<E>> {
  if (!secret) throw new WebhookVerificationError("Missing webhook secret");
  if (typeof rawBody !== "string" && !(rawBody instanceof Uint8Array) && !(rawBody instanceof ArrayBuffer)) {
    throw new WebhookVerificationError(
      "rawBody must be the raw request body (string or bytes), not a parsed object",
    );
  }
  const { timestamp, signatures } = parseHeader(signatureHeader);

  const tolerance = options.toleranceSeconds ?? 300;
  const now =
    options.now instanceof Date
      ? Math.floor(options.now.getTime() / 1000)
      : options.now ?? Math.floor(Date.now() / 1000);
  if (Math.abs(now - timestamp) > tolerance) {
    throw new WebhookVerificationError("Timestamp outside the tolerance (replay protection)");
  }

  const bodyBytes =
    typeof rawBody === "string"
      ? encoder.encode(rawBody)
      : rawBody instanceof ArrayBuffer
        ? new Uint8Array(rawBody)
        : rawBody;
  const prefix = encoder.encode(`${timestamp}.`);
  const message = new Uint8Array(prefix.length + bodyBytes.length);
  message.set(prefix, 0);
  message.set(bodyBytes, prefix.length);

  const crypto = await subtle();
  const key = await crypto.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const expected = toHex(await crypto.sign("HMAC", key, message));

  let match = false;
  for (const signature of signatures) {
    // Evaluate every entry so the time does not reveal which one matched.
    if (timingSafeEqual(signature, expected)) match = true;
  }
  if (!match) throw new WebhookVerificationError("Signature mismatch");

  const text = typeof rawBody === "string" ? rawBody : new TextDecoder().decode(bodyBytes);
  let payload: unknown;
  try {
    payload = JSON.parse(text);
  } catch {
    throw new WebhookVerificationError("Body is not valid JSON");
  }
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    throw new WebhookVerificationError("Body is not a JSON object");
  }
  return payload as WebhookPayload<E>;
}
