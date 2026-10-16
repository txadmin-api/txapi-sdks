"""Live console and server-log streams over WebSocket (SPEC §9).

Requires the optional ``websockets`` dependency: ``pip install "txapi-sdk[live]"``.

Streams are asyncio-based. Both :class:`txapi.TxApi` and :class:`txapi.AsyncTxApi`
hand them out (``client.console.connect()``), but they must be used inside a
running event loop::

    async with client.console.connect() as console:
        print(await console.send_command("status"))
        async for event in console:
            if isinstance(event, LogEvent):
                print(event.item)
"""

from __future__ import annotations

import asyncio
import collections
import inspect
import json
import logging
import random
from collections.abc import AsyncIterator, Callable
from dataclasses import dataclass, field
from types import TracebackType
from typing import Any, ClassVar, Generic, Literal, TypeVar, Union
from urllib.parse import quote

from ._errors import APIConnectionError, APITimeoutError, CommandError, ValidationError
from .types import ServerLogEntry

__all__ = [
    "MAX_COMMAND_LENGTH",
    "RECONNECT_CLOSE_CODES",
    "CloseEvent",
    "ConsoleEvent",
    "ConsoleStream",
    "ErrorEvent",
    "FrameEvent",
    "InitEvent",
    "LiveEvent",
    "LogEvent",
    "OpenEvent",
    "ReconnectingEvent",
    "ServerLogEvent",
    "ServerLogStream",
]

logger = logging.getLogger("txapi.live")

T = TypeVar("T")

MAX_COMMAND_LENGTH = 1024
"""Longest console command the API accepts."""

RECONNECT_CLOSE_CODES = frozenset({1001, 1006, 1011, 1012})
"""Close codes after which a stream reconnects automatically. Never 1008/1009."""

COMMAND_TIMEOUT = 10.0
_MAX_FRAME_BYTES = 32 * 1024 * 1024


# ─────────────────────────────────────────────────────────────────── events


@dataclass(frozen=True)
class OpenEvent:
    """The WebSocket is connected. ``reconnected`` is ``True`` after an automatic reconnect."""

    reconnected: bool
    type: Literal["open"] = "open"


@dataclass(frozen=True)
class InitEvent(Generic[T]):
    """The server's current buffer, sent right after (re)connecting.

    After a reconnect this is the *full* buffer again — de-duplicate or ignore it.
    Not sent when the buffer is empty.
    """

    items: list[T]
    type: Literal["init"] = "init"


@dataclass(frozen=True)
class LogEvent(Generic[T]):
    """One new console line (``str``) or server-log entry (``dict`` or ``str``)."""

    item: T
    type: Literal["log"] = "log"


@dataclass(frozen=True)
class ErrorEvent:
    """An ``error`` frame not answering a pending :meth:`ConsoleStream.send_command`."""

    message: str
    type: Literal["error"] = "error"


@dataclass(frozen=True)
class CloseEvent:
    """The connection closed.

    Attributes:
        code: WebSocket close code (``None`` when no connection was established).
        reason: Close reason sent by the server (for 1008 it tells you what to fix).
        will_reconnect: ``True`` if the stream reconnects automatically.
        by_user: ``True`` after :meth:`close`.
    """

    code: int | None
    reason: str
    will_reconnect: bool
    by_user: bool = False
    type: Literal["close"] = "close"


@dataclass(frozen=True)
class ReconnectingEvent:
    """The stream will reconnect after ``delay`` seconds (``attempt`` starts at 1)."""

    attempt: int
    delay: float
    code: int | None
    reason: str
    type: Literal["reconnecting"] = "reconnecting"


@dataclass(frozen=True)
class FrameEvent:
    """A raw server frame (only emitted with ``raw_frames=True``)."""

    frame: dict[str, Any]
    type: Literal["frame"] = "frame"


LiveEvent = Union[
    OpenEvent, InitEvent[T], LogEvent[T], ErrorEvent, CloseEvent, ReconnectingEvent, FrameEvent
]
ConsoleEvent = Union[
    OpenEvent,
    InitEvent[str],
    LogEvent[str],
    ErrorEvent,
    CloseEvent,
    ReconnectingEvent,
    FrameEvent,
]
ServerLogEvent = Union[
    OpenEvent,
    InitEvent[ServerLogEntry],
    LogEvent[ServerLogEntry],
    ErrorEvent,
    CloseEvent,
    ReconnectingEvent,
    FrameEvent,
]

EventCallback = Callable[[Any], Any]


@dataclass(frozen=True)
class LiveConfig:
    base_url: str
    api_key: str = field(repr=False)
    server_id: int | str | None
    user_agent: str


class _End:
    pass


_END = _End()


# ─────────────────────────────────────────────────────────────────── stream


class _LiveStream(Generic[T]):
    _path: ClassVar[str]
    _items_key: ClassVar[str]

    def __init__(
        self,
        config: LiveConfig,
        *,
        raw_frames: bool = False,
        on_event: EventCallback | None = None,
        reconnect: bool = True,
        max_reconnect_attempts: int | None = None,
        initial_reconnect_delay: float = 1.0,
        max_reconnect_delay: float = 30.0,
        open_timeout: float = 10.0,
        max_queued_events: int = 10_000,
    ) -> None:
        self._config = config
        self._raw_frames = raw_frames
        self._on_event = on_event
        self._reconnect = reconnect
        self._max_attempts = max_reconnect_attempts
        self._initial_delay = initial_reconnect_delay
        self._max_delay = max_reconnect_delay
        self._open_timeout = open_timeout
        self._queue: collections.deque[LiveEvent[T] | _End] = collections.deque(
            maxlen=max_queued_events
        )
        self._wakeup: asyncio.Event | None = None
        self._task: asyncio.Task[None] | None = None
        self._ws: Any = None
        self._closing = False
        self._ended = False
        self._final_emitted = False
        self.close_code: int | None = None
        """Code of the last close (``None`` before the first disconnect)."""
        self.close_reason: str = ""
        """Reason of the last close, e.g. why the server refused the connection (1008)."""
        self.dropped_events = 0
        """Events dropped because more than ``max_queued_events`` were waiting to be read."""

    # ── public API

    @property
    def connected(self) -> bool:
        """``True`` while a WebSocket connection is open."""
        return self._ws is not None

    def __repr__(self) -> str:
        return (
            f"{type(self).__name__}(path={self._path!r}, server_id={self._config.server_id!r},"
            f" connected={self.connected})"
        )

    async def start(self) -> None:
        """Start connecting in the background (called by ``async with`` and iteration)."""
        self._ensure_started()

    async def close(self) -> None:
        """Close the stream; no reconnect afterwards and iteration ends."""
        if self._closing:
            if self._task is not None and not self._task.done():
                await asyncio.gather(self._task, return_exceptions=True)
            return
        self._closing = True
        ws = self._ws
        if ws is not None:
            try:
                await ws.close(1000, "closed by client")
            except Exception:  # pragma: no cover - best effort
                pass
        if self._task is not None and not self._task.done():
            self._task.cancel()
            await asyncio.gather(self._task, return_exceptions=True)
        self._ws = None
        self._fail_pending(APIConnectionError("stream closed"))
        if not self._final_emitted:
            self._final_emitted = True
            self.close_code = 1000
            self.close_reason = "closed by client"
            await self._emit(CloseEvent(1000, "closed by client", False, by_user=True))
        self._finish()

    async def __aenter__(self: _S) -> _S:
        await self.start()
        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None,
        exc: BaseException | None,
        tb: TracebackType | None,
    ) -> None:
        await self.close()

    def __aiter__(self) -> AsyncIterator[LiveEvent[T]]:
        self._ensure_started()
        return self._events()

    # ── internals

    def _ensure_started(self) -> None:
        if self._wakeup is None:
            self._wakeup = asyncio.Event()
        if self._task is None and not self._closing:
            self._task = asyncio.get_running_loop().create_task(self._run())

    async def _events(self) -> AsyncIterator[LiveEvent[T]]:
        assert self._wakeup is not None
        while True:
            if self._queue:
                item = self._queue.popleft()
                if isinstance(item, _End):
                    self._queue.appendleft(item)  # keep the end marker for other readers
                    return
                yield item
                continue
            self._wakeup.clear()
            await self._wakeup.wait()

    def _push(self, item: LiveEvent[T] | _End) -> None:
        if self._queue.maxlen is not None and len(self._queue) == self._queue.maxlen:
            self.dropped_events += 1
        self._queue.append(item)
        if self._wakeup is not None:
            self._wakeup.set()

    async def _emit(self, event: LiveEvent[T]) -> None:
        self._push(event)
        if self._on_event is not None:
            try:
                result = self._on_event(event)
                if inspect.isawaitable(result):
                    await result
            except Exception:
                logger.exception("txapi: on_event callback raised")

    def _finish(self) -> None:
        if not self._ended:
            self._ended = True
            self._push(_END)

    def _url(self) -> str:
        base = self._config.base_url
        if base.startswith("https://"):
            base = "wss://" + base[len("https://") :]
        elif base.startswith("http://"):
            base = "ws://" + base[len("http://") :]
        url = f"{base}{self._path}?apiKey={quote(self._config.api_key, safe='')}"
        if self._config.server_id is not None:
            url += f"&serverId={quote(str(self._config.server_id), safe='')}"
        return url

    def _redact(self, text: str) -> str:
        key = self._config.api_key
        for variant in {key, quote(key, safe="")}:
            if variant:
                text = text.replace(variant, "***")
        return text

    def _delay(self, attempt: int) -> float:
        base = min(self._max_delay, self._initial_delay * (2.0 ** (attempt - 1)))
        return base * (0.5 + random.random() / 2)

    def _fail_pending(self, error: Exception) -> None:
        """Hook for streams with pending requests (console commands)."""

    async def _handle_frame(self, frame: dict[str, Any]) -> None:
        kind = frame.get("type")
        if kind == "init":
            items = frame.get(self._items_key)
            await self._emit(InitEvent(list(items) if isinstance(items, list) else []))
        elif kind == "log":
            items = frame.get(self._items_key)
            for item in items if isinstance(items, list) else []:
                await self._emit(LogEvent(item))
        elif kind == "error":
            await self._on_error_frame(str(frame.get("message", "")))
        elif kind == "ack":
            self._on_ack(str(frame.get("message", "")))

    async def _on_error_frame(self, message: str) -> None:
        await self._emit(ErrorEvent(message))

    def _on_ack(self, message: str) -> None:
        """Hook for streams that send commands."""

    async def _run(self) -> None:
        try:
            await self._loop()
        except asyncio.CancelledError:
            pass
        except Exception as exc:  # pragma: no cover - defensive
            logger.exception("txapi: live stream failed")
            if not self._final_emitted:
                self._final_emitted = True
                await self._emit(CloseEvent(None, self._redact(str(exc)), False))
        finally:
            self._ws = None
            self._fail_pending(APIConnectionError("live connection closed"))
            if not self._closing:
                self._finish()

    async def _loop(self) -> None:
        # Imported lazily so the REST client works without the optional dependency.
        try:
            from websockets.asyncio.client import connect
            from websockets.exceptions import ConnectionClosed, InvalidStatus
        except ImportError as exc:  # pragma: no cover - depends on environment
            raise ImportError(
                'Live streams need the websockets package: pip install "txapi-sdk[live]"'
            ) from exc

        attempt = 0
        reconnected = False
        while not self._closing:
            code: int | None = None
            reason = ""
            try:
                ws = await connect(
                    self._url(),
                    additional_headers={"User-Agent": self._config.user_agent},
                    open_timeout=self._open_timeout,
                    max_size=_MAX_FRAME_BYTES,
                )
            except InvalidStatus as exc:
                status = exc.response.status_code
                reason = f"HTTP {status} during WebSocket handshake"
                if 400 <= status < 500 and status != 429:
                    await self._final(None, reason)
                    return
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                reason = self._redact(f"{type(exc).__name__}: {exc}")
            else:
                if self._closing:
                    await ws.close(1000, "closed by client")
                    return
                self._ws = ws
                attempt = 0
                await self._emit(OpenEvent(reconnected))
                reconnected = True
                try:
                    async for raw in ws:
                        frame = _decode(raw)
                        if frame is None:
                            continue
                        if self._raw_frames:
                            await self._emit(FrameEvent(frame))
                        await self._handle_frame(frame)
                except ConnectionClosed:
                    pass
                code = ws.close_code if ws.close_code is not None else 1006
                reason = ws.close_reason or ""
                self._ws = None
                self._fail_pending(
                    APIConnectionError(f"live connection closed ({code}) before the ack")
                )
                if self._closing:
                    return
                self.close_code, self.close_reason = code, reason
                will = self._should_reconnect(code, attempt + 1)
                await self._emit(CloseEvent(code, reason, will))
                if not will:
                    self._final_emitted = True
                    return

            attempt += 1
            if not self._should_reconnect(code, attempt):
                if code is None:
                    await self._final(None, reason)
                return
            delay = self._delay(attempt)
            await self._emit(ReconnectingEvent(attempt, delay, code, reason))
            await asyncio.sleep(delay)

    def _should_reconnect(self, code: int | None, attempt: int) -> bool:
        if self._closing or not self._reconnect:
            return False
        if code is not None and code not in RECONNECT_CLOSE_CODES:
            return False
        return self._max_attempts is None or attempt <= self._max_attempts

    async def _final(self, code: int | None, reason: str) -> None:
        self.close_code, self.close_reason = code, reason
        self._final_emitted = True
        await self._emit(CloseEvent(code, reason, False))


_S = TypeVar("_S", bound="_LiveStream[Any]")


def _decode(raw: str | bytes) -> dict[str, Any] | None:
    try:
        frame = json.loads(raw)
    except ValueError:
        logger.warning("txapi: ignoring non-JSON live frame")
        return None
    return frame if isinstance(frame, dict) else None


class ConsoleStream(_LiveStream[str]):
    """Live console of a server (``/ws/console``, permission ``console.write``).

    Yields :data:`ConsoleEvent` values: :class:`OpenEvent`, :class:`InitEvent` (buffer),
    one :class:`LogEvent` per console line, :class:`ErrorEvent`, :class:`CloseEvent`,
    :class:`ReconnectingEvent` and (with ``raw_frames=True``) :class:`FrameEvent`.
    """

    _path = "/ws/console"
    _items_key = "lines"

    def __init__(
        self, config: LiveConfig, *, command_timeout: float = COMMAND_TIMEOUT, **options: Any
    ) -> None:
        super().__init__(config, **options)
        self._command_timeout = command_timeout
        self._pending: collections.deque[asyncio.Future[str]] = collections.deque()
        # Created lazily: on Python 3.9 asyncio primitives bind to the loop at creation.
        self._send_lock: asyncio.Lock | None = None

    async def send_command(self, command: str, *, timeout: float | None = None) -> str:
        """Run a console command and wait for txAdmin's ``ack``.

        The command's *output* arrives later as normal :class:`LogEvent` lines; the
        ack only means txAdmin accepted it. Commands are correlated FIFO.

        Returns:
            The ack message.

        Raises:
            ValidationError: Empty command or longer than 1024 characters.
            CommandError: The server answered with an ``error`` frame
                (rate limit, quota, txAdmin unreachable …).
            APITimeoutError: No answer within ``timeout`` (default 10 s).
            APIConnectionError: Not connected, or the connection dropped first.
        """
        if not isinstance(command, str) or not command.strip():
            raise ValidationError("console command must not be empty")
        if len(command) > MAX_COMMAND_LENGTH:
            raise ValidationError(
                f"console command is {len(command)} characters long, "
                f"the API accepts at most {MAX_COMMAND_LENGTH}"
            )
        ws = self._ws
        if ws is None:
            raise APIConnectionError("live console is not connected")
        future: asyncio.Future[str] = asyncio.get_running_loop().create_future()
        if self._send_lock is None:
            self._send_lock = asyncio.Lock()
        async with self._send_lock:
            self._pending.append(future)
            try:
                await ws.send(json.dumps({"type": "command", "command": command}))
            except Exception as exc:
                if future in self._pending:
                    self._pending.remove(future)
                raise APIConnectionError(
                    f"could not send the command: {type(exc).__name__}"
                ) from exc
        wait = self._command_timeout if timeout is None else timeout
        try:
            return await asyncio.wait_for(asyncio.shield(future), wait)
        except asyncio.TimeoutError:
            # Keep the slot: a late ack still belongs to this command (FIFO).
            future.cancel()
            raise APITimeoutError(f"no answer to the console command within {wait:g} s") from None

    def _next_pending(self) -> asyncio.Future[str] | None:
        while self._pending:
            future = self._pending.popleft()
            if not future.done():
                return future
            # Expired (timed out): this answer belonged to it — consume and stop.
            return None
        return None

    def _on_ack(self, message: str) -> None:
        if not self._pending:
            return
        future = self._next_pending()
        if future is not None:
            future.set_result(message)

    async def _on_error_frame(self, message: str) -> None:
        if self._pending:
            future = self._next_pending()
            if future is not None:
                future.set_exception(CommandError(message or "command failed"))
            return
        await self._emit(ErrorEvent(message))

    def _fail_pending(self, error: Exception) -> None:
        while self._pending:
            future = self._pending.popleft()
            if not future.done():
                future.set_exception(error)


class ServerLogStream(_LiveStream[ServerLogEntry]):
    """Live server log (``/ws/serverlog``, permission ``logs.view``). Read-only.

    Yields :data:`ServerLogEvent` values; :class:`LogEvent` items are txAdmin's
    structured entries (``dict``) or plain strings.
    """

    _path = "/ws/serverlog"
    _items_key = "entries"
