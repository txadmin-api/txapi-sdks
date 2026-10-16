"""Exception hierarchy of the txApi SDK (SPEC §4).

Every exception raised by the SDK derives from :class:`TxApiError`.
None of them ever contains the API key: messages come from the API's
``error`` text, the response body or the HTTP reason phrase.
"""

from __future__ import annotations

from typing import Any

import httpx

from ._ratelimit import RateLimit

__all__ = [
    "APIConnectionError",
    "APITimeoutError",
    "AuthenticationError",
    "BadRequestError",
    "CommandError",
    "ConflictError",
    "NotFoundError",
    "PermissionDeniedError",
    "RateLimitError",
    "ServerError",
    "TxAdminRejectedError",
    "TxAdminUnavailableError",
    "TxApiError",
    "ValidationError",
    "WebhookVerificationError",
]


class TxApiError(Exception):
    """Base class of every error raised by the SDK.

    Attributes:
        message: Human-readable message (the API's ``error`` text when available).
        status: HTTP status code, ``None`` for errors without a response.
        body: Parsed response body (``dict``/``list``/``str``), ``None`` without a response.
        headers: Response headers, ``None`` without a response.
        rate_limit: Rate-limit state of the failed response (SPEC §6), if sent.
    """

    message: str
    status: int | None
    body: Any
    headers: httpx.Headers | None
    rate_limit: RateLimit | None

    def __init__(
        self,
        message: str,
        *,
        status: int | None = None,
        body: Any = None,
        headers: httpx.Headers | None = None,
        rate_limit: RateLimit | None = None,
    ) -> None:
        super().__init__(message)
        self.message = message
        self.status = status
        self.body = body
        self.headers = headers
        self.rate_limit = rate_limit

    def __str__(self) -> str:
        if self.status is not None:
            return f"{self.status}: {self.message}"
        return self.message

    def __repr__(self) -> str:
        return f"{type(self).__name__}(status={self.status!r}, message={self.message!r})"


class BadRequestError(TxApiError):
    """HTTP 400, 405 or 415 — the request was invalid."""


class AuthenticationError(TxApiError):
    """HTTP 401 — key missing, invalid, revoked or expired."""


class PermissionDeniedError(TxApiError):
    """HTTP 403 — plan feature or key scope missing, IP not allowed, server disabled,
    or the txAdmin account lacks a permission."""


class NotFoundError(TxApiError):
    """HTTP 404 — unknown player, action, admin or server."""


class ConflictError(TxApiError):
    """HTTP 409 — e.g. no txAdmin server configured."""


class TxAdminRejectedError(TxApiError):
    """HTTP 422 — txAdmin refused the action (e.g. the player is offline)."""


class RateLimitError(TxApiError):
    """HTTP 429.

    Attributes:
        retry_after: Seconds from ``Retry-After``; ``None`` when the header is missing.
        quota_exhausted: ``True`` when ``Retry-After`` is missing — the monthly quota
            is used up (it resets on the 1st of the month, 00:00 UTC). Retrying will not help.
    """

    retry_after: float | None
    quota_exhausted: bool

    def __init__(
        self,
        message: str,
        *,
        retry_after: float | None = None,
        status: int | None = 429,
        body: Any = None,
        headers: httpx.Headers | None = None,
        rate_limit: RateLimit | None = None,
    ) -> None:
        super().__init__(message, status=status, body=body, headers=headers, rate_limit=rate_limit)
        self.retry_after = retry_after
        self.quota_exhausted = retry_after is None


class ServerError(TxApiError):
    """HTTP 5xx (other than 502)."""


class TxAdminUnavailableError(ServerError):
    """HTTP 502 — txAdmin is unreachable or answered garbage."""


class APIConnectionError(TxApiError):
    """Transport problem: DNS, refused connection, reset, TLS … (spec name: ``ConnectionError``)."""


class APITimeoutError(APIConnectionError):
    """The request (or a live command) timed out (spec name: ``TimeoutError``)."""


class ValidationError(TxApiError, ValueError):
    """Client-side check failed; no request was sent (SPEC §2)."""


class WebhookVerificationError(TxApiError):
    """A webhook delivery failed signature verification (SPEC §8).

    Attributes:
        reason: Machine-friendly reason, e.g. ``"signature_mismatch"``.
    """

    reason: str

    def __init__(self, message: str, *, reason: str) -> None:
        super().__init__(message)
        self.reason = reason


class CommandError(TxApiError):
    """The live console answered a :meth:`~txapi.live.ConsoleStream.send_command`
    with an ``error`` frame (empty/too long command, rate limit, quota, txAdmin down)."""


_STATUS_ERRORS: dict[int, type[TxApiError]] = {
    400: BadRequestError,
    405: BadRequestError,
    415: BadRequestError,
    401: AuthenticationError,
    403: PermissionDeniedError,
    404: NotFoundError,
    409: ConflictError,
    422: TxAdminRejectedError,
    502: TxAdminUnavailableError,
}


def error_class_for_status(status: int) -> type[TxApiError]:
    """Exception class for an HTTP error status (SPEC §4)."""
    if status == 429:
        return RateLimitError
    known = _STATUS_ERRORS.get(status)
    if known is not None:
        return known
    if status >= 500:
        return ServerError
    return TxApiError
