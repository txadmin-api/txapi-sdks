"""Auto-paging iterators (SPEC §7).

Pages are fetched lazily: nothing is requested until the iterator is consumed,
and each further page only when the previous one is exhausted.
"""

from __future__ import annotations

from collections.abc import AsyncIterator, Awaitable, Callable, Iterator
from typing import Any, Optional

from .types import Event, EventPage, HistoryPage, HistoryRow, PlayerPage, PlayerRow

__all__ = ["player_sort_key"]

_SORT_ALIASES = {
    "playTime": "playTime",
    "tsJoined": "tsJoined",
    "tsLastConnection": "tsLastConnection",
    "sessions": "tsJoined",
    "lastConnection": "tsLastConnection",
}


def player_sort_key(sort_by: str | None) -> str:
    """Row field holding the sort-key value for ``sort_by`` (default ``playTime``)."""
    if sort_by is None or not sort_by.strip():
        return "playTime"
    # Unknown values are rejected by the API (400) on the first page.
    return _SORT_ALIASES.get(sort_by.strip(), sort_by.strip())


PlayerFetch = Callable[[Optional[int], Optional[str]], PlayerPage]
HistoryFetch = Callable[[Optional[int], Optional[str]], HistoryPage]
EventFetch = Callable[[Optional[str]], EventPage]
AsyncPlayerFetch = Callable[[Optional[int], Optional[str]], Awaitable[PlayerPage]]
AsyncHistoryFetch = Callable[[Optional[int], Optional[str]], Awaitable[HistoryPage]]
AsyncEventFetch = Callable[[Optional[str]], Awaitable[EventPage]]


def _next_player_cursor(page: PlayerPage, sort_key: str) -> tuple[int, str] | None:
    rows = page.get("players") or []
    if not rows or page.get("hasReachedEnd", True):
        return None
    last: Any = rows[-1]
    offset = last.get(sort_key)
    license_ = last.get("license")
    if offset is None or not license_:
        return None
    return int(offset), str(license_)


def _next_history_cursor(page: HistoryPage) -> tuple[int, str] | None:
    rows = page.get("history") or []
    if not rows or page.get("hasReachedEnd", True):
        return None
    last: Any = rows[-1]
    offset = last.get("timestamp")
    action_id = last.get("id")
    if offset is None or not action_id:
        return None
    return int(offset), str(action_id)


def _next_event_cursor(page: EventPage) -> str | None:
    if not page.get("events"):
        return None
    cursor = page.get("nextCursor")
    return cursor if isinstance(cursor, str) and cursor else None


def iter_players(fetch: PlayerFetch, sort_key: str) -> Iterator[PlayerRow]:
    offset: int | None = None
    cursor: str | None = None
    while True:
        page = fetch(offset, cursor)
        yield from page.get("players") or []
        following = _next_player_cursor(page, sort_key)
        if following is None:
            return
        offset, cursor = following


def iter_history(fetch: HistoryFetch) -> Iterator[HistoryRow]:
    offset: int | None = None
    cursor: str | None = None
    while True:
        page = fetch(offset, cursor)
        yield from page.get("history") or []
        following = _next_history_cursor(page)
        if following is None:
            return
        offset, cursor = following


def iter_events(fetch: EventFetch) -> Iterator[Event]:
    cursor: str | None = None
    while True:
        page = fetch(cursor)
        yield from page.get("events") or []
        cursor = _next_event_cursor(page)
        if cursor is None:
            return


async def aiter_players(fetch: AsyncPlayerFetch, sort_key: str) -> AsyncIterator[PlayerRow]:
    offset: int | None = None
    cursor: str | None = None
    while True:
        page = await fetch(offset, cursor)
        for row in page.get("players") or []:
            yield row
        following = _next_player_cursor(page, sort_key)
        if following is None:
            return
        offset, cursor = following


async def aiter_history(fetch: AsyncHistoryFetch) -> AsyncIterator[HistoryRow]:
    offset: int | None = None
    cursor: str | None = None
    while True:
        page = await fetch(offset, cursor)
        for row in page.get("history") or []:
            yield row
        following = _next_history_cursor(page)
        if following is None:
            return
        offset, cursor = following


async def aiter_events(fetch: AsyncEventFetch) -> AsyncIterator[Event]:
    cursor: str | None = None
    while True:
        page = await fetch(cursor)
        for row in page.get("events") or []:
            yield row
        cursor = _next_event_cursor(page)
        if cursor is None:
            return
