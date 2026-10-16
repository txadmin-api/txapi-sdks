import type { RateLimit } from "./ratelimit";

/** Options accepted by every {@link TxApiError} constructor. */
export interface TxApiErrorInit {
  status?: number | undefined;
  body?: unknown;
  headers?: Headers | undefined;
  rateLimit?: RateLimit | null | undefined;
  cause?: unknown;
}

/**
 * Base class of every error the SDK throws.
 *
 * HTTP errors carry `status`, the parsed `body`, the response `headers` and the
 * `rateLimit` of that response. Transport and client-side errors leave them undefined.
 * The API key never appears in a message.
 */
export class TxApiError extends Error {
  /** HTTP status code, `undefined` for transport/client-side errors. */
  readonly status: number | undefined;
  /** Parsed response body (JSON value or text). */
  readonly body: unknown;
  /** Response headers. */
  readonly headers: Headers | undefined;
  /** Rate-limit headers of the failing response, if any. */
  readonly rateLimit: RateLimit | null;

  constructor(message: string, init: TxApiErrorInit = {}) {
    super(message, init.cause !== undefined ? { cause: init.cause } : undefined);
    this.name = new.target.name;
    this.status = init.status;
    this.body = init.body;
    this.headers = init.headers;
    this.rateLimit = init.rateLimit ?? null;
  }
}

/** 400, 405, 415 — invalid input. */
export class BadRequestError extends TxApiError {}
/** 401 — key missing, invalid, revoked or expired. */
export class AuthenticationError extends TxApiError {}
/** 403 — plan feature / key scope missing, IP not allowed, server disabled, or the txAdmin account lacks a permission. */
export class PermissionDeniedError extends TxApiError {}
/** 404 — unknown player, action, admin or server. */
export class NotFoundError extends TxApiError {}
/** 409 — conflict, e.g. no server configured. */
export class ConflictError extends TxApiError {}
/** 422 — txAdmin refused the action (e.g. the player is offline). */
export class TxAdminRejectedError extends TxApiError {}
/** 5xx other than 502. */
export class ServerError extends TxApiError {}
/** 502 — txAdmin unreachable or answered garbage. */
export class TxAdminUnavailableError extends TxApiError {}

/**
 * 429 — too many requests.
 *
 * `retryAfter` is the server's `Retry-After` in seconds. When it is missing, the
 * **monthly quota** is exhausted (`quotaExhausted = true`): it resets on the 1st of the
 * month, 00:00 UTC, so waiting a few seconds will not help.
 */
export class RateLimitError extends TxApiError {
  /** Seconds to wait before retrying, or `null` when the server sent no `Retry-After`. */
  readonly retryAfter: number | null;
  /** `true` when there was no `Retry-After` — the monthly quota is used up. */
  readonly quotaExhausted: boolean;

  constructor(message: string, init: TxApiErrorInit & { retryAfter: number | null }) {
    super(message, init);
    this.retryAfter = init.retryAfter;
    this.quotaExhausted = init.retryAfter === null;
  }
}

/** The connection failed (DNS, refused, reset, TLS …). No HTTP response was received. */
export class ConnectionError extends TxApiError {}

/** The request (or a WebSocket command) did not complete within the timeout. */
export class TimeoutError extends TxApiError {}

/** A client-side check failed before anything was sent (missing required field, too long …). */
export class ValidationError extends TxApiError {
  /** Name of the offending field or parameter. */
  readonly field: string | undefined;

  constructor(message: string, field?: string) {
    super(message);
    this.field = field;
  }
}

/** A webhook delivery failed signature/timestamp verification. `message` holds the reason. */
export class WebhookVerificationError extends TxApiError {}

/** The server answered a live-console command with an `error` frame. */
export class CommandRejectedError extends TxApiError {}

/**
 * A live stream (WebSocket) was closed for good by the server — e.g. 1008
 * (authentication/permission/quota/too slow) or 1009. `reason` is the server's close reason.
 */
export class StreamClosedError extends TxApiError {
  /** WebSocket close code. */
  readonly code: number;
  /** Close reason sent by the server (may be empty). */
  readonly reason: string;

  constructor(code: number, reason: string) {
    super(reason ? `Stream closed (${code}): ${reason}` : `Stream closed (${code})`);
    this.code = code;
    this.reason = reason;
  }
}

function messageFrom(status: number, statusText: string, body: unknown): string {
  if (body && typeof body === "object" && !Array.isArray(body)) {
    const record = body as Record<string, unknown>;
    if (typeof record.error === "string" && record.error.trim() !== "") return record.error;
    if (typeof record.message === "string" && record.message.trim() !== "") return record.message;
    try {
      return JSON.stringify(body).slice(0, 500);
    } catch {
      /* fall through */
    }
  }
  if (typeof body === "string" && body.trim() !== "") return body.trim().slice(0, 500);
  if (statusText) return statusText;
  return `HTTP ${status}`;
}

/** Builds the matching error subclass for an HTTP error response. */
export function errorFromResponse(
  status: number,
  statusText: string,
  body: unknown,
  headers: Headers,
  rateLimit: RateLimit | null,
  retryAfter: number | null,
): TxApiError {
  const message = messageFrom(status, statusText, body);
  const init: TxApiErrorInit = { status, body, headers, rateLimit };
  switch (status) {
    case 400:
    case 405:
    case 415:
      return new BadRequestError(message, init);
    case 401:
      return new AuthenticationError(message, init);
    case 403:
      return new PermissionDeniedError(message, init);
    case 404:
      return new NotFoundError(message, init);
    case 409:
      return new ConflictError(message, init);
    case 422:
      return new TxAdminRejectedError(message, init);
    case 429:
      return new RateLimitError(message, { ...init, retryAfter });
    case 502:
      return new TxAdminUnavailableError(message, init);
    default:
      if (status >= 500) return new ServerError(message, init);
      return new TxApiError(message, init);
  }
}
