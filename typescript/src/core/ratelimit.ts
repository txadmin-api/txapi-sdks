/**
 * Rate-limit information the API sends with every response
 * (`X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`).
 */
export interface RateLimit {
  /** Requests allowed per window (per minute). `null` when the header was missing or unparsable. */
  limit: number | null;
  /** Requests left in the current window. */
  remaining: number | null;
  /** Seconds until the window resets. */
  resetSeconds: number | null;
}

function intHeader(headers: Headers, name: string): number | null {
  const raw = headers.get(name);
  if (raw === null) return null;
  const trimmed = raw.trim();
  if (!/^-?\d+$/.test(trimmed)) return null;
  const n = Number(trimmed);
  return Number.isSafeInteger(n) ? n : null;
}

/** Parses the rate-limit headers; returns `null` when none of them is present. */
export function parseRateLimit(headers: Headers): RateLimit | null {
  const limit = intHeader(headers, "x-ratelimit-limit");
  const remaining = intHeader(headers, "x-ratelimit-remaining");
  const resetSeconds = intHeader(headers, "x-ratelimit-reset");
  if (
    headers.get("x-ratelimit-limit") === null &&
    headers.get("x-ratelimit-remaining") === null &&
    headers.get("x-ratelimit-reset") === null
  ) {
    return null;
  }
  return { limit, remaining, resetSeconds };
}

/**
 * Parses `Retry-After` (delta seconds or an HTTP date) into seconds.
 * Returns `null` when absent or unparsable.
 */
export function parseRetryAfter(headers: Headers, nowMs: number = Date.now()): number | null {
  const raw = headers.get("retry-after");
  if (raw === null) return null;
  const trimmed = raw.trim();
  if (trimmed === "") return null;
  if (/^\d+(\.\d+)?$/.test(trimmed)) return Number(trimmed);
  const date = Date.parse(trimmed);
  if (Number.isNaN(date)) return null;
  return Math.max(0, Math.ceil((date - nowMs) / 1000));
}
