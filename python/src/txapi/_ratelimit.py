"""Rate-limit headers (``X-RateLimit-*``) and ``Retry-After`` parsing."""

from __future__ import annotations

import email.utils
import time
from collections.abc import Mapping
from dataclasses import dataclass

__all__ = ["RateLimit", "parse_rate_limit", "parse_retry_after"]


@dataclass(frozen=True)
class RateLimit:
    """Per-minute rate-limit state reported by the API on every response.

    Attributes:
        limit: Requests allowed per minute (``X-RateLimit-Limit``), ``None`` if not sent.
        remaining: Requests left in the current window (``X-RateLimit-Remaining``).
        reset_seconds: Seconds until the window resets (``X-RateLimit-Reset``).
    """

    limit: int | None
    remaining: int | None
    reset_seconds: int | None


def _header(headers: Mapping[str, str], name: str) -> str | None:
    value = headers.get(name)
    if value is None:
        # Plain dicts are case sensitive; httpx.Headers is not.
        lower = name.lower()
        for key, candidate in headers.items():
            if key.lower() == lower:
                return candidate
    return value


def _int(value: str | None) -> int | None:
    if value is None:
        return None
    try:
        return int(value.strip())
    except ValueError:
        try:
            return int(float(value.strip()))
        except ValueError:
            return None


def parse_rate_limit(headers: Mapping[str, str]) -> RateLimit | None:
    """Parse ``X-RateLimit-Limit/-Remaining/-Reset``; ``None`` if none of them is sent."""
    limit = _int(_header(headers, "X-RateLimit-Limit"))
    remaining = _int(_header(headers, "X-RateLimit-Remaining"))
    reset = _int(_header(headers, "X-RateLimit-Reset"))
    if limit is None and remaining is None and reset is None:
        return None
    return RateLimit(limit=limit, remaining=remaining, reset_seconds=reset)


def parse_retry_after(headers: Mapping[str, str], now: float | None = None) -> float | None:
    """Parse ``Retry-After`` (seconds or HTTP date) to seconds; ``None`` if absent/invalid."""
    raw = _header(headers, "Retry-After")
    if raw is None:
        return None
    raw = raw.strip()
    if not raw:
        return None
    try:
        seconds = float(raw)
    except ValueError:
        try:
            parsed = email.utils.parsedate_to_datetime(raw)
        except (TypeError, ValueError, IndexError):
            return None
        if parsed is None:
            return None
        seconds = parsed.timestamp() - (time.time() if now is None else now)
    if seconds != seconds:  # NaN
        return None
    return max(0.0, seconds)
