"""Verification of outgoing txApi webhooks (SPEC §8)."""

from __future__ import annotations

import hashlib
import hmac
import json
import time
from typing import cast

from ._errors import WebhookVerificationError
from .types import WebhookEvent, WebhookEventName

__all__ = [
    "DEFAULT_TOLERANCE_SECONDS",
    "SIGNATURE_HEADER",
    "WEBHOOK_EVENTS",
    "WebhookEventName",
    "verify_webhook",
]

SIGNATURE_HEADER = "X-Txapi-Signature"
"""Header carrying ``t=<unix seconds>,v1=<hex HMAC-SHA256>``."""
EVENT_HEADER = "X-Txapi-Event"
DELIVERY_HEADER = "X-Txapi-Delivery"
DEFAULT_TOLERANCE_SECONDS = 300

WEBHOOK_EVENTS: tuple[WebhookEventName, ...] = (
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
)
"""Every event name a webhook can deliver (``spec/endpoints.json -> webhookEvents``)."""


def _fail(message: str, reason: str) -> WebhookVerificationError:
    return WebhookVerificationError(f"webhook verification failed: {message}", reason=reason)


def verify_webhook(
    raw_body: bytes | str,
    signature_header: str | None,
    secret: str,
    tolerance_seconds: float = DEFAULT_TOLERANCE_SECONDS,
    now: float | None = None,
) -> WebhookEvent:
    """Verify a webhook delivery and return the parsed event.

    Args:
        raw_body: The request body **exactly as received** (bytes). Parsing the JSON
            and serialising it again changes the bytes and breaks the signature.
        signature_header: Value of the ``X-Txapi-Signature`` header.
        secret: The endpoint's signing secret (``whsec_…``) from the dashboard.
        tolerance_seconds: Maximum age (and clock skew) of the timestamp ``t``.
        now: Current Unix time in seconds (for tests); defaults to ``time.time()``.

    Returns:
        ``{id, event, createdAt, serverId, data}``.

    Raises:
        WebhookVerificationError: Header malformed, signature mismatch, timestamp
            outside the tolerance or body not JSON. ``reason`` tells which.
    """
    if not secret:
        raise _fail("no secret configured", "missing_secret")
    body = raw_body.encode("utf-8") if isinstance(raw_body, str) else bytes(raw_body)

    timestamp: int | None = None
    signatures: list[str] = []
    for part in (signature_header or "").split(","):
        key, sep, value = part.strip().partition("=")
        if not sep:
            continue
        key, value = key.strip(), value.strip()
        if key == "t":
            try:
                timestamp = int(value)
            except ValueError:
                raise _fail("timestamp is not a number", "malformed_header") from None
        elif key == "v1" and value:
            signatures.append(value.lower())
    if timestamp is None:
        raise _fail("no timestamp (t=) in the signature header", "missing_timestamp")
    if not signatures:
        raise _fail("no signature (v1=) in the signature header", "missing_signature")

    current = time.time() if now is None else now
    if abs(current - timestamp) > tolerance_seconds:
        raise _fail("timestamp outside the tolerance (replay?)", "timestamp_out_of_tolerance")

    expected = (
        hmac.new(secret.encode("utf-8"), f"{timestamp}.".encode() + body, hashlib.sha256)
        .hexdigest()
        .encode("ascii")
    )
    # Compare bytes: compare_digest raises TypeError for non-ASCII str input.
    if not any(
        hmac.compare_digest(expected, candidate.encode("utf-8")) for candidate in signatures
    ):
        raise _fail("signature does not match", "signature_mismatch")

    try:
        event = json.loads(body)
    except ValueError:
        raise _fail("body is not valid JSON", "invalid_body") from None
    if not isinstance(event, dict):
        raise _fail("body is not a JSON object", "invalid_body")
    return cast(WebhookEvent, event)
