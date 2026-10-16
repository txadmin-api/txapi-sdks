"""Official Python SDK for the txApi REST and WebSocket API (txAdmin / FiveM).

Quickstart::

    from txapi import TxApi

    client = TxApi(server_id=1)            # key from the TXAPI_KEY environment variable
    page = client.players.search(query="john")
    client.players.ban(identifier="license:" + page["players"][0]["license"],
                       reason="Cheating", duration_hours=168)

See https://txapi.cloud/docs for the API reference.
"""

from __future__ import annotations

from . import types
from ._client import AsyncTxApi, RequestHook, ResponseHook, TxApi
from ._core import ApiResponse
from ._errors import (
    APIConnectionError,
    APITimeoutError,
    AuthenticationError,
    BadRequestError,
    CommandError,
    ConflictError,
    NotFoundError,
    PermissionDeniedError,
    RateLimitError,
    ServerError,
    TxAdminRejectedError,
    TxAdminUnavailableError,
    TxApiError,
    ValidationError,
    WebhookVerificationError,
)
from ._ratelimit import RateLimit
from ._version import API_VERSION, __version__
from .types import WebhookEvent, WebhookEventName
from .webhooks import WEBHOOK_EVENTS, verify_webhook

VERSION = __version__
"""Alias of ``__version__`` (the SDK version)."""

__all__ = [
    "API_VERSION",
    "VERSION",
    "WEBHOOK_EVENTS",
    "APIConnectionError",
    "APITimeoutError",
    "ApiResponse",
    "AsyncTxApi",
    "AuthenticationError",
    "BadRequestError",
    "CommandError",
    "ConflictError",
    "NotFoundError",
    "PermissionDeniedError",
    "RateLimit",
    "RateLimitError",
    "RequestHook",
    "ResponseHook",
    "ServerError",
    "TxAdminRejectedError",
    "TxAdminUnavailableError",
    "TxApi",
    "TxApiError",
    "ValidationError",
    "WebhookEvent",
    "WebhookEventName",
    "WebhookVerificationError",
    "__version__",
    "types",
    "verify_webhook",
]
