"""SPEC §5: retries."""

from __future__ import annotations

import asyncio
import time
from collections.abc import Iterator

import httpx
import pytest

import txapi
from txapi import _core

from .conftest import Recorder


def _sequence(*responses: httpx.Response | Exception) -> Recorder:
    items: Iterator[httpx.Response | Exception] = iter(responses)

    def handler(request: httpx.Request) -> httpx.Response:
        item = next(items)
        if isinstance(item, Exception):
            raise item
        return item

    return Recorder(handler=handler)


def ok() -> httpx.Response:
    return httpx.Response(200, json={"success": True, "message": "ok"})


def test_429_with_retry_after_retries_any_method(sleeps: list[float]) -> None:
    recorder = _sequence(
        httpx.Response(429, headers={"Retry-After": "3"}, json={"error": "x"}), ok()
    )
    result = recorder.sync_client().players.ban(identifier="license:abc", reason="x")
    assert result["success"] is True
    assert len(recorder.requests) == 2
    assert sleeps == [3.0]
    assert recorder.requests[0].content == recorder.requests[1].content


def test_429_retry_after_http_date(sleeps: list[float]) -> None:
    import email.utils

    when = email.utils.formatdate(time.time() + 10, usegmt=True)
    recorder = _sequence(httpx.Response(429, headers={"Retry-After": when}), ok())
    recorder.sync_client().server.status()
    assert len(sleeps) == 1
    assert 5 <= sleeps[0] <= 10


def test_429_without_retry_after_is_never_retried(sleeps: list[float]) -> None:
    recorder = _sequence(httpx.Response(429, json={"error": "monthly quota exhausted"}))
    with pytest.raises(txapi.RateLimitError) as info:
        recorder.sync_client(max_retries=5).server.status()
    assert info.value.quota_exhausted
    assert len(recorder.requests) == 1
    assert sleeps == []


def test_429_retry_after_above_60_is_not_waited(sleeps: list[float]) -> None:
    recorder = _sequence(httpx.Response(429, headers={"Retry-After": "61"}))
    with pytest.raises(txapi.RateLimitError):
        recorder.sync_client().server.status()
    assert sleeps == []


@pytest.mark.parametrize("status", [502, 503, 504])
def test_5xx_get_is_retried(sleeps: list[float], status: int) -> None:
    recorder = _sequence(httpx.Response(status), httpx.Response(status), ok())
    assert recorder.sync_client().server.status() == {"success": True, "message": "ok"}
    assert len(recorder.requests) == 3
    assert len(sleeps) == 2
    assert all(0 <= s <= 8 for s in sleeps)


def test_500_is_not_retried(sleeps: list[float]) -> None:
    recorder = _sequence(httpx.Response(500))
    with pytest.raises(txapi.ServerError):
        recorder.sync_client().server.status()
    assert len(recorder.requests) == 1


def test_502_post_is_not_retried(sleeps: list[float]) -> None:
    recorder = _sequence(httpx.Response(502, json={"error": "txAdmin down"}))
    with pytest.raises(txapi.TxAdminUnavailableError):
        recorder.sync_client().players.ban(identifier="license:abc", reason="x")
    assert len(recorder.requests) == 1
    assert sleeps == []


def test_connection_error_get_retried_post_not(sleeps: list[float]) -> None:
    def refused() -> httpx.ConnectError:
        return httpx.ConnectError("refused")

    recorder = _sequence(refused(), ok())
    recorder.sync_client().server.status()
    assert len(recorder.requests) == 2

    recorder = _sequence(refused())
    with pytest.raises(txapi.APIConnectionError):
        recorder.sync_client().server.restart()
    assert len(recorder.requests) == 1


def test_retries_exhausted_raises_last_error(sleeps: list[float]) -> None:
    recorder = _sequence(*(httpx.Response(503) for _ in range(5)))
    with pytest.raises(txapi.ServerError):
        recorder.sync_client(max_retries=2).server.status()
    assert len(recorder.requests) == 3
    assert len(sleeps) == 2


def test_max_retries_zero_disables_everything(sleeps: list[float]) -> None:
    recorder = _sequence(httpx.Response(429, headers={"Retry-After": "1"}))
    with pytest.raises(txapi.RateLimitError):
        recorder.sync_client(max_retries=0).server.status()
    recorder = _sequence(httpx.Response(502))
    with pytest.raises(txapi.TxAdminUnavailableError):
        recorder.sync_client(max_retries=0).server.status()
    assert sleeps == []


def test_retry_after_overrides_backoff_for_5xx(sleeps: list[float]) -> None:
    recorder = _sequence(httpx.Response(503, headers={"Retry-After": "2"}), ok())
    recorder.sync_client().server.status()
    assert sleeps == [2.0]


def test_backoff_formula() -> None:
    assert _core.backoff_delay(0, rand=lambda: 1.0) == 0.5
    assert _core.backoff_delay(1, rand=lambda: 1.0) == 1.0
    assert _core.backoff_delay(3, rand=lambda: 1.0) == 4.0
    assert _core.backoff_delay(4, rand=lambda: 1.0) == 8.0
    assert _core.backoff_delay(10, rand=lambda: 1.0) == 8.0
    assert _core.backoff_delay(10, rand=lambda: 0.0) == 0.0


def test_hooks_called_per_attempt_and_cannot_break_requests(sleeps: list[float]) -> None:
    recorder = _sequence(httpx.Response(503), ok())
    seen: list[str] = []

    def on_request(request: httpx.Request) -> None:
        seen.append(f"req {request.method}")
        raise RuntimeError("hook bug")

    def on_response(response: httpx.Response) -> None:
        seen.append(f"res {response.status_code}")
        raise RuntimeError("hook bug")

    client = recorder.sync_client(on_request=on_request, on_response=on_response)
    client.server.status()
    assert seen == ["req GET", "res 503", "req GET", "res 200"]


async def test_async_retries(sleeps: list[float]) -> None:
    recorder = _sequence(
        httpx.Response(429, headers={"Retry-After": "1"}), httpx.Response(502), ok()
    )
    async with recorder.async_client() as client:
        await client.server.status()
    assert len(recorder.requests) == 3
    assert sleeps[0] == 1.0

    recorder = _sequence(httpx.Response(502))
    async with recorder.async_client() as client:
        with pytest.raises(txapi.TxAdminUnavailableError):
            await client.server.kick_all()
    assert len(recorder.requests) == 1


async def test_async_hooks_may_be_coroutines(sleeps: list[float]) -> None:
    recorder = Recorder()
    seen: list[int] = []

    async def on_response(response: httpx.Response) -> None:
        seen.append(response.status_code)
        raise RuntimeError("ignored")

    async with recorder.async_client(on_response=on_response) as client:
        await client.server.status()
    assert seen == [200]


async def test_async_cancellation_stops_waiting_immediately() -> None:
    recorder = _sequence(httpx.Response(429, headers={"Retry-After": "30"}), ok())
    client = recorder.async_client()
    task = asyncio.ensure_future(client.server.status())
    for _ in range(50):
        await asyncio.sleep(0.01)
        if recorder.requests:
            break
    started = time.monotonic()
    task.cancel()
    with pytest.raises(asyncio.CancelledError):
        await task
    assert time.monotonic() - started < 1
    assert len(recorder.requests) == 1
    await client.close()
