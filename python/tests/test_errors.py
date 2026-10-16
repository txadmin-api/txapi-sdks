"""SPEC §4: error mapping."""

from __future__ import annotations

import httpx
import pytest

import txapi

from .conftest import KEY, Recorder


@pytest.mark.parametrize(
    ("status", "cls"),
    [
        (400, txapi.BadRequestError),
        (405, txapi.BadRequestError),
        (415, txapi.BadRequestError),
        (401, txapi.AuthenticationError),
        (403, txapi.PermissionDeniedError),
        (404, txapi.NotFoundError),
        (409, txapi.ConflictError),
        (422, txapi.TxAdminRejectedError),
        (429, txapi.RateLimitError),
        (500, txapi.ServerError),
        (502, txapi.TxAdminUnavailableError),
        (503, txapi.ServerError),
        (504, txapi.ServerError),
        (418, txapi.TxApiError),
    ],
)
def test_status_maps_to_error_class(recorder: Recorder, status: int, cls: type) -> None:
    recorder.handler = lambda r: httpx.Response(
        status, json={"success": False, "error": "nope"}, headers={"X-RateLimit-Limit": "60"}
    )
    client = recorder.sync_client(max_retries=0)
    with pytest.raises(cls) as info:
        client.players.ban(identifier="license:abc", reason="x")
    error = info.value
    assert type(error) is cls
    assert isinstance(error, txapi.TxApiError)
    assert error.status == status
    assert error.message == "nope"
    assert error.body == {"success": False, "error": "nope"}
    assert error.headers is not None
    assert error.rate_limit == txapi.RateLimit(limit=60, remaining=None, reset_seconds=None)
    assert str(error) == f"{status}: nope"


def test_txadmin_unavailable_is_a_server_error() -> None:
    assert issubclass(txapi.TxAdminUnavailableError, txapi.ServerError)
    assert issubclass(txapi.APITimeoutError, txapi.APIConnectionError)


def test_message_falls_back_to_plain_text_body(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(
        502, text="<html>Bad Gateway</html>", headers={"Content-Type": "text/html"}
    )
    client = recorder.sync_client(max_retries=0)
    with pytest.raises(txapi.TxAdminUnavailableError) as info:
        client.players.ban(identifier="license:abc", reason="x")
    assert info.value.message == "<html>Bad Gateway</html>"
    assert info.value.body == "<html>Bad Gateway</html>"


def test_message_from_error_only_body(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(404, json={"error": "player not found"})
    with pytest.raises(txapi.NotFoundError, match="player not found"):
        recorder.sync_client().players.info(identifier="license:x")


def test_message_falls_back_to_reason_phrase(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(403)
    with pytest.raises(txapi.PermissionDeniedError) as info:
        recorder.sync_client().players.info(identifier="license:x")
    assert info.value.message == "Forbidden"
    assert info.value.body is None


def test_plain_text_success_body(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(200, text="pong")
    assert recorder.sync_client().server.drops() == "pong"


def test_errors_never_contain_the_key(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(401, json={"error": "API key is invalid"})
    with pytest.raises(txapi.AuthenticationError) as info:
        recorder.sync_client().account.me()
    assert KEY not in str(info.value)
    assert KEY not in repr(info.value)


def test_rate_limit_error_quota_vs_retry_after(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(429, json={"error": "quota"})
    client = recorder.sync_client()
    with pytest.raises(txapi.RateLimitError) as info:
        client.account.me()
    assert info.value.quota_exhausted is True
    assert info.value.retry_after is None
    assert len(recorder.requests) == 1  # quota: never retried

    recorder.requests.clear()
    recorder.handler = lambda r: httpx.Response(
        429, json={"error": "slow down"}, headers={"Retry-After": "120"}
    )
    with pytest.raises(txapi.RateLimitError) as info:
        client.account.me()
    assert info.value.quota_exhausted is False
    assert info.value.retry_after == 120
    assert len(recorder.requests) == 1  # > 60 s is not waited for


def test_connection_error(recorder: Recorder) -> None:
    def fail(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("refused", request=request)

    recorder.handler = fail
    with pytest.raises(txapi.APIConnectionError) as info:
        recorder.sync_client(max_retries=0).server.status()
    assert info.value.status is None
    assert not isinstance(info.value, txapi.APITimeoutError)


def test_timeout_error(recorder: Recorder) -> None:
    def fail(request: httpx.Request) -> httpx.Response:
        raise httpx.ReadTimeout("slow", request=request)

    recorder.handler = fail
    with pytest.raises(txapi.APITimeoutError):
        recorder.sync_client(max_retries=0).server.status()


async def test_async_error_mapping(recorder: Recorder) -> None:
    recorder.handler = lambda r: httpx.Response(422, json={"success": False, "error": "offline"})
    async with recorder.async_client() as client:
        with pytest.raises(txapi.TxAdminRejectedError, match="offline"):
            await client.players.kick(identifier="license:abc")


async def test_async_timeout(recorder: Recorder) -> None:
    def fail(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectTimeout("slow", request=request)

    recorder.handler = fail
    async with recorder.async_client(max_retries=0) as client:
        with pytest.raises(txapi.APITimeoutError):
            await client.server.status()
