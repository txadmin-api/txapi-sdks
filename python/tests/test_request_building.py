"""SPEC §1/§2: configuration, URL building, encoding, query and body rules, validation."""

from __future__ import annotations

import json
from typing import Any

import httpx
import pytest

import txapi

from .conftest import BASE, KEY, Recorder


def _query(request: httpx.Request) -> list[tuple[str, str]]:
    return list(request.url.params.multi_items())


def _body(request: httpx.Request) -> Any:
    return json.loads(request.content) if request.content else None


# ───────────────────────────────────────────────────────────── configuration


def test_missing_key_fails_at_construction() -> None:
    with pytest.raises(txapi.ValidationError, match="TXAPI_KEY"):
        txapi.TxApi()
    with pytest.raises(txapi.ValidationError):
        txapi.AsyncTxApi(api_key="   ")


def test_key_and_base_url_from_environment(
    monkeypatch: pytest.MonkeyPatch, recorder: Recorder
) -> None:
    monkeypatch.setenv("TXAPI_KEY", KEY)
    monkeypatch.setenv("TXAPI_BASE_URL", "https://env.example/")
    client = txapi.TxApi(http_client=httpx.Client(transport=httpx.MockTransport(recorder)))
    assert client.base_url == "https://env.example"
    client.account.me()
    assert str(recorder.last.url) == "https://env.example/api/v1/me"
    assert recorder.last.headers["Authorization"] == f"Bearer {KEY}"


def test_defaults() -> None:
    client = txapi.TxApi(api_key=KEY)
    assert client.base_url == "https://txapi.cloud"
    assert client.timeout == 30.0
    assert client.max_retries == 2
    assert client.server_id is None
    client.close()


def test_trailing_slash_stripped_and_headers(recorder: Recorder) -> None:
    client = recorder.sync_client(base_url="https://api.test///", user_agent="mybot/2")
    client.account.servers()
    request = recorder.last
    assert str(request.url) == "https://api.test/api/v1/servers"
    assert request.headers["Authorization"] == f"Bearer {KEY}"
    assert request.headers["Accept"] == "application/json"
    assert request.headers["User-Agent"] == f"txapi-python/{txapi.__version__} mybot/2"


def test_invalid_base_url_and_retries() -> None:
    with pytest.raises(txapi.ValidationError):
        txapi.TxApi(api_key=KEY, base_url="ftp://x")
    with pytest.raises(txapi.ValidationError):
        txapi.TxApi(api_key=KEY, max_retries=-1)


def test_repr_never_contains_the_key(recorder: Recorder) -> None:
    client = recorder.sync_client(server_id=3)
    aclient = recorder.async_client()
    assert KEY not in repr(client)
    assert KEY not in str(client)
    assert KEY not in repr(aclient)
    assert "redacted" in repr(client)
    assert KEY not in repr(client._config)
    assert KEY not in repr(client._live_config(None))


def test_versions() -> None:
    assert txapi.__version__ == "1.0.0"
    assert txapi.VERSION == "1.0.0"
    assert txapi.API_VERSION == "1.8.0"


# ───────────────────────────────────────────────────────────── path encoding


def test_path_parameters_are_percent_encoded(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.admins.get("name with space")
    client.whitelist.approvals.remove("license:abc")
    client.history.revoke("A/B?C#D")
    raw = [r.url.raw_path.split(b"?")[0] for r in recorder.requests]
    assert raw == [
        b"/api/v1/txadmin/admins/name%20with%20space",
        b"/api/v1/whitelist/approvals/license%3Aabc",
        b"/api/v1/history/revoke/A%2FB%3FC%23D",
    ]


def test_blank_path_parameter_is_rejected(recorder: Recorder) -> None:
    client = recorder.sync_client()
    with pytest.raises(txapi.ValidationError, match="name"):
        client.admins.get("  ")
    assert recorder.requests == []


# ───────────────────────────────────────────────────────────── serverId


def test_server_id_default_and_override(recorder: Recorder) -> None:
    client = recorder.sync_client(server_id=5)
    client.server.status()
    client.server.status(server_id="9")
    assert _query(recorder.requests[0]) == [("serverId", "5")]
    assert _query(recorder.requests[1]) == [("serverId", "9")]


def test_server_id_omitted_for_account_level(recorder: Recorder) -> None:
    client = recorder.sync_client(server_id=5)
    client.account.me()
    client.account.servers()
    assert all(_query(r) == [] for r in recorder.requests)


def test_server_id_absent_without_default(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.server.status()
    assert _query(recorder.last) == []


# ───────────────────────────────────────────────────────────── query


def test_query_none_omitted_booleans_and_camel_case(recorder: Recorder) -> None:
    client = recorder.sync_client(server_id=1)
    client.players.search(query="john", sort_by="tsJoined", filters=["isOnline", "hasNote"])
    assert _query(recorder.last) == [
        ("query", "john"),
        ("sortBy", "tsJoined"),
        ("filters", "isOnline,hasNote"),
        ("serverId", "1"),
    ]
    client.events.deaths(pvp=True, limit=10)
    assert _query(recorder.last) == [("pvp", "true"), ("limit", "10"), ("serverId", "1")]
    client.events.deaths(pvp=False)
    assert ("pvp", "false") in _query(recorder.last)
    client.history.search(search_type="identifiers", search="license:abc")
    assert _query(recorder.last)[:2] == [("searchType", "identifiers"), ("search", "license:abc")]


def test_csv_query_accepts_plain_string(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.events.list(type="death,kill")
    assert _query(recorder.last) == [("type", "death,kill")]


def test_query_values_are_encoded(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.players.info(identifier="license:a b&c=d")
    assert recorder.last.url.params["identifier"] == "license:a b&c=d"


def test_required_query_parameter_validated(recorder: Recorder) -> None:
    client = recorder.sync_client()
    with pytest.raises(txapi.ValidationError, match="identifier"):
        client.players.info(identifier="")
    assert recorder.requests == []


# ───────────────────────────────────────────────────────────── bodies


def test_body_camel_case_and_none_omitted(recorder: Recorder) -> None:
    client = recorder.sync_client(server_id=2)
    client.players.ban(identifier="license:abc", reason="Cheating", duration_hours=24)
    request = recorder.last
    assert request.method == "POST"
    assert request.headers["Content-Type"] == "application/json"
    assert _body(request) == {
        "identifier": "license:abc",
        "reason": "Cheating",
        "durationHours": 24,
    }
    assert _query(request) == [("serverId", "2")]


def test_discord_flags(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.players.ban(identifier="discord:1", reason="x", discord_ban=True)
    assert _body(recorder.last)["discordBan"] is True
    client.history.revoke("ABCD-1234", reason="appeal", discord_unban=True)
    assert _body(recorder.last) == {"reason": "appeal", "discordUnban": True}


def test_false_boolean_is_sent(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.players.whitelist(identifier="license:abc", whitelisted=False)
    assert _body(recorder.last) == {"identifier": "license:abc", "whitelisted": False}
    client.server.skip_restart(skip=False)
    assert _body(recorder.last) == {"skip": False}


def test_partial_update_sends_only_given_fields(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.admins.edit("Bob", discord_id="123")
    assert recorder.last.method == "PUT"
    assert _body(recorder.last) == {"discordId": "123"}
    client.admins.edit("Bob")
    assert _body(recorder.last) == {}


def test_list_body_field(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.admins.add(name="Bob", permissions=("players.ban", "console.write"))
    assert _body(recorder.last) == {"name": "Bob", "permissions": ["players.ban", "console.write"]}


def test_no_body_for_bodiless_post(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.server.restart()
    assert recorder.last.method == "POST"
    assert recorder.last.content == b""
    assert "Content-Type" not in recorder.last.headers


@pytest.mark.parametrize(
    ("call", "match"),
    [
        (lambda c: c.players.ban(identifier="license:abc", reason="   "), "reason"),
        (lambda c: c.players.ban(identifier="", reason="x"), "identifier"),
        (lambda c: c.players.ban(identifier="license:abc", reason="x" * 513), "512"),
        (lambda c: c.console.command(command="x" * 1025), "1024"),
        (lambda c: c.players.notes(identifier="license:abc", note="n" * 2001), "2000"),
        (lambda c: c.server.resources.restart(name="r" * 129), "128"),
        (lambda c: c.admins.add(name="Bob", permissions=[]), "permissions"),
    ],
)
def test_validation_errors_send_nothing(recorder: Recorder, call: Any, match: str) -> None:
    client = recorder.sync_client()
    with pytest.raises(txapi.ValidationError, match=match):
        call(client)
    assert recorder.requests == []


def test_max_length_is_inclusive(recorder: Recorder) -> None:
    client = recorder.sync_client()
    client.players.ban(identifier="license:abc", reason="x" * 512)
    client.console.command(command="y" * 1024)
    assert len(recorder.requests) == 2


def test_validation_error_is_a_value_error() -> None:
    assert issubclass(txapi.ValidationError, ValueError)
    assert issubclass(txapi.ValidationError, txapi.TxApiError)


async def test_async_builds_the_same_request(recorder: Recorder) -> None:
    async with recorder.async_client(server_id=4) as client:
        await client.players.ban(identifier="license:abc", reason="r", duration_hours=1)
        await client.admins.get("a b")
        with pytest.raises(txapi.ValidationError):
            await client.players.ban(identifier="license:abc", reason="")
    ban, admin = recorder.requests
    assert _body(ban) == {"identifier": "license:abc", "reason": "r", "durationHours": 1}
    assert _query(ban) == [("serverId", "4")]
    assert admin.url.raw_path == b"/api/v1/txadmin/admins/a%20b?serverId=4"


def test_per_call_timeout(recorder: Recorder) -> None:
    client = recorder.sync_client(timeout=12)
    client.server.status()
    assert recorder.last.extensions["timeout"]["read"] == 12
    client.server.status(timeout=3.5)
    assert recorder.last.extensions["timeout"]["read"] == 3.5


def test_injected_http_client_is_not_closed() -> None:
    http = httpx.Client(transport=httpx.MockTransport(Recorder()))
    with txapi.TxApi(api_key=KEY, base_url=BASE, http_client=http) as client:
        client.account.me()
    assert not http.is_closed
    owned = txapi.TxApi(api_key=KEY)
    owned.close()
    assert owned._http.is_closed


async def test_async_injected_http_client_is_not_closed() -> None:
    http = httpx.AsyncClient(transport=httpx.MockTransport(Recorder()))
    async with txapi.AsyncTxApi(api_key=KEY, base_url=BASE, http_client=http) as client:
        await client.account.me()
    assert not http.is_closed
    await http.aclose()
