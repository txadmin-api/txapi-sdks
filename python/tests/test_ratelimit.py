"""SPEC §6: rate-limit headers, ``last_rate_limit`` and raw response access."""

from __future__ import annotations

import httpx
import pytest

import txapi
from txapi._ratelimit import parse_rate_limit, parse_retry_after

from .conftest import Recorder

HEADERS = {"X-RateLimit-Limit": "120", "X-RateLimit-Remaining": "117", "X-RateLimit-Reset": "42"}


def test_parse_rate_limit() -> None:
    assert parse_rate_limit(httpx.Headers(HEADERS)) == txapi.RateLimit(120, 117, 42)
    assert parse_rate_limit({"x-ratelimit-remaining": "3"}) == txapi.RateLimit(None, 3, None)
    assert parse_rate_limit({}) is None
    assert parse_rate_limit({"X-RateLimit-Limit": "abc"}) is None


def test_parse_retry_after() -> None:
    assert parse_retry_after({"Retry-After": "7"}) == 7.0
    assert parse_retry_after({"Retry-After": "0"}) == 0.0
    assert parse_retry_after({}) is None
    assert parse_retry_after({"Retry-After": ""}) is None
    assert parse_retry_after({"Retry-After": "soon"}) is None
    date = "Thu, 09 Oct 2025 08:54:00 GMT"
    assert parse_retry_after({"Retry-After": date}, now=1760000000) == 40.0
    assert parse_retry_after({"Retry-After": date}, now=1760001000) == 0.0


def test_last_rate_limit_is_tracked(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(200, json={}, headers=HEADERS)
    client = recorder.sync_client()
    assert client.last_rate_limit is None
    client.server.status()
    assert client.last_rate_limit == txapi.RateLimit(limit=120, remaining=117, reset_seconds=42)


def test_rate_limit_on_errors(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(
        429, json={"error": "slow"}, headers={**HEADERS, "X-RateLimit-Remaining": "0"}
    )
    client = recorder.sync_client()
    with pytest.raises(txapi.RateLimitError) as info:
        client.server.status()
    assert info.value.rate_limit == txapi.RateLimit(120, 0, 42)
    assert client.last_rate_limit == txapi.RateLimit(120, 0, 42)


def test_with_raw_response(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(
        200, json={"success": True, "message": "Banned"}, headers=HEADERS
    )
    client = recorder.sync_client()
    raw = client.with_raw_response.players.ban(identifier="license:abc", reason="x")
    assert isinstance(raw, txapi.ApiResponse)
    assert raw.data == {"success": True, "message": "Banned"}
    assert raw.status_code == 200
    assert raw.headers["X-RateLimit-Remaining"] == "117"
    assert raw.rate_limit == txapi.RateLimit(120, 117, 42)
    assert isinstance(raw.http_response, httpx.Response)


async def test_async_with_raw_response(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(200, json={"players": []}, headers=HEADERS)
    async with recorder.async_client() as client:
        raw = await client.with_raw_response.players.online()
        assert raw.data == {"players": []}
        assert raw.rate_limit == txapi.RateLimit(120, 117, 42)
        assert client.last_rate_limit == raw.rate_limit
