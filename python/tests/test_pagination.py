"""SPEC §7: auto-paging iterators."""

from __future__ import annotations

from collections.abc import Callable
from typing import Any

import httpx
import pytest

from txapi._pagination import player_sort_key

from .conftest import Recorder

Pages = Callable[[httpx.Request], dict[str, Any]]


def _pages(*pages: dict[str, Any]) -> Recorder:
    remaining = list(pages)

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=remaining.pop(0))

    return Recorder(handler=handler)


def _params(recorder: Recorder) -> list[dict[str, str]]:
    return [dict(r.url.params) for r in recorder.requests]


def _player(license_: str, **values: int) -> dict[str, Any]:
    return {"license": license_, "displayName": license_, **values}


@pytest.mark.parametrize(
    ("sort_by", "field"),
    [
        (None, "playTime"),
        ("playTime", "playTime"),
        ("tsJoined", "tsJoined"),
        ("sessions", "tsJoined"),
        ("tsLastConnection", "tsLastConnection"),
        ("lastConnection", "tsLastConnection"),
    ],
)
def test_player_sort_key(sort_by: str | None, field: str) -> None:
    assert player_sort_key(sort_by) == field


def test_iter_search_uses_sort_key_and_license() -> None:
    recorder = _pages(
        {
            "players": [
                _player("a", playTime=10, tsJoined=900),
                _player("b", playTime=9, tsJoined=800),
            ],
            "hasReachedEnd": False,
        },
        {"players": [_player("c", playTime=5, tsJoined=700)], "hasReachedEnd": True},
    )
    client = recorder.sync_client(server_id=1)
    rows = list(client.players.iter_search(query="j", sort_by="sessions"))
    assert [r["license"] for r in rows] == ["a", "b", "c"]
    first, second = _params(recorder)
    assert "offset" not in first
    assert "cursor" not in first
    assert second["offset"] == "800"  # tsJoined of the last row ("sessions" alias)
    assert second["cursor"] == "b"
    assert second["sortBy"] == "sessions"
    assert second["query"] == "j"
    assert second["serverId"] == "1"


def test_iter_online_and_lookup_sort_by_play_time() -> None:
    recorder = _pages(
        {"players": [_player("a", playTime=50)], "hasReachedEnd": False},
        {"players": [], "hasReachedEnd": False},
    )
    rows = list(recorder.sync_client().players.iter_online())
    assert [r["license"] for r in rows] == ["a"]
    assert _params(recorder)[1]["offset"] == "50"
    assert len(recorder.requests) == 2  # empty page stops even without hasReachedEnd

    recorder = _pages({"players": [_player("x", playTime=1)], "hasReachedEnd": True})
    list(recorder.sync_client().players.iter_lookup(identifier="discord:1"))
    assert _params(recorder)[0]["identifier"] == "discord:1"


def test_iteration_is_lazy() -> None:
    recorder = _pages(
        {"players": [_player("a", playTime=2), _player("b", playTime=1)], "hasReachedEnd": False},
        {"players": [_player("c", playTime=0)], "hasReachedEnd": True},
    )
    iterator = recorder.sync_client().players.iter_search()
    assert recorder.requests == []
    assert next(iterator)["license"] == "a"
    assert next(iterator)["license"] == "b"
    assert len(recorder.requests) == 1
    assert next(iterator)["license"] == "c"
    assert len(recorder.requests) == 2
    assert list(iterator) == []


def test_missing_cursor_value_stops() -> None:
    recorder = _pages({"players": [{"displayName": "no license"}], "hasReachedEnd": False})
    assert len(list(recorder.sync_client().players.iter_search())) == 1
    assert len(recorder.requests) == 1


def test_iter_history_uses_timestamp_and_id() -> None:
    recorder = _pages(
        {
            "history": [
                {"id": "AAAA-1111", "timestamp": 1700000300, "type": "ban"},
                {"id": "BBBB-2222", "timestamp": 1700000200, "type": "ban"},
            ],
            "hasReachedEnd": False,
        },
        {"history": [{"id": "CCCC-3333", "timestamp": 1700000100}], "hasReachedEnd": True},
    )
    rows = list(recorder.sync_client().history.iter_bans())
    assert [r["id"] for r in rows] == ["AAAA-1111", "BBBB-2222", "CCCC-3333"]
    second = _params(recorder)[1]
    assert second["offset"] == "1700000200"
    assert second["cursor"] == "BBBB-2222"


def test_iter_history_search_keeps_filters() -> None:
    recorder = _pages(
        {"history": [{"id": "A-1", "timestamp": 5}], "hasReachedEnd": False},
        {"history": [], "hasReachedEnd": True},
    )
    list(recorder.sync_client().history.iter_search(type="warn", admin="Josef"))
    second = _params(recorder)[1]
    assert second["type"] == "warn"
    assert second["admin"] == "Josef"
    assert second["offset"] == "5"


def test_iter_events_follows_next_cursor() -> None:
    recorder = _pages(
        {"events": [{"id": 3}, {"id": 2}], "nextCursor": "1759838400123_2"},
        {"events": [{"id": 1}]},
    )
    rows = list(recorder.sync_client().events.iter_list(type=["death", "kill"], limit=2))
    assert [r["id"] for r in rows] == [3, 2, 1]
    first, second = _params(recorder)
    assert "cursor" not in first
    assert second["cursor"] == "1759838400123_2"
    assert second["type"] == "death,kill"
    assert second["limit"] == "2"


@pytest.mark.parametrize(
    "method",
    [
        "iter_deaths",
        "iter_kills",
        "iter_chat",
        "iter_commands",
        "iter_admin_actions",
        "iter_actions",
        "iter_sessions",
    ],
)
def test_every_event_iterator(method: str) -> None:
    recorder = _pages(
        {"events": [{"id": 1}], "nextCursor": "c1"}, {"events": [], "nextCursor": "c2"}
    )
    rows = list(getattr(recorder.sync_client().events, method)())
    assert rows == [{"id": 1}]
    assert len(recorder.requests) == 2


async def test_async_iterators() -> None:
    recorder = _pages(
        {"players": [_player("a", tsLastConnection=9)], "hasReachedEnd": False},
        {"players": [_player("b", tsLastConnection=8)], "hasReachedEnd": True},
        {"history": [{"id": "A-1", "timestamp": 3}], "hasReachedEnd": False},
        {"history": [{"id": "B-2", "timestamp": 2}]},
        {"events": [{"id": 9}], "nextCursor": "n"},
        {"events": [{"id": 8}]},
    )
    async with recorder.async_client() as client:
        players = [r["license"] async for r in client.players.iter_search(sort_by="lastConnection")]
        history = [r["id"] async for r in client.history.iter_warns()]
        events = [r["id"] async for r in client.events.iter_admin_actions()]
    assert players == ["a", "b"]
    assert history == ["A-1", "B-2"]  # missing hasReachedEnd is treated as the end
    assert events == [9, 8]
    params = _params(recorder)
    assert params[1]["offset"] == "9"
    assert params[3]["cursor"] == "A-1"
    assert params[5]["cursor"] == "n"


async def test_async_iteration_is_lazy() -> None:
    recorder = _pages({"events": [{"id": 1}], "nextCursor": "x"}, {"events": [{"id": 2}]})
    async with recorder.async_client() as client:
        iterator = client.events.iter_list()
        assert recorder.requests == []
        assert (await iterator.__anext__())["id"] == 1
        assert len(recorder.requests) == 1
