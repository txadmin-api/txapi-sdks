"""SPEC §9: live streams against a local WebSocket server."""

from __future__ import annotations

import asyncio
import json
import socket
from collections.abc import Awaitable, Callable
from typing import Any

import pytest
from websockets.asyncio.server import Server, ServerConnection, serve
from websockets.http11 import Request, Response

import txapi
from txapi.live import (
    CloseEvent,
    ConsoleStream,
    ErrorEvent,
    FrameEvent,
    InitEvent,
    LogEvent,
    OpenEvent,
    ReconnectingEvent,
    ServerLogStream,
)

from .conftest import KEY

Behaviour = Callable[[ServerConnection, int], Awaitable[None]]

FAST: dict[str, Any] = {
    "initial_reconnect_delay": 0.01,
    "max_reconnect_delay": 0.05,
    "open_timeout": 2.0,
}


class LiveServer:
    """Local WebSocket server; ``behaviour(ws, n)`` runs for the n-th connection (0-based)."""

    def __init__(self, behaviour: Behaviour, *, reject_status: int | None = None) -> None:
        self.behaviour = behaviour
        self.reject_status = reject_status
        self.paths: list[str] = []
        self.received: list[dict[str, Any]] = []
        self.closed = asyncio.Event()
        self._server: Server | None = None

    async def _process_request(
        self, connection: ServerConnection, request: Request
    ) -> Response | None:
        self.paths.append(request.path)
        if self.reject_status is not None:
            return connection.respond(self.reject_status, "Forbidden\n")
        return None

    async def _handler(self, ws: ServerConnection) -> None:
        index = len(self.paths) - 1
        try:
            await self.behaviour(ws, index)
        finally:
            self.closed.set()

    async def __aenter__(self) -> LiveServer:
        self._server = await serve(
            self._handler, "127.0.0.1", 0, process_request=self._process_request
        )
        return self

    async def __aexit__(self, *exc: object) -> None:
        assert self._server is not None
        self._server.close()
        await self._server.wait_closed()

    @property
    def base_url(self) -> str:
        assert self._server is not None
        port = next(iter(self._server.sockets)).getsockname()[1]
        return f"http://127.0.0.1:{port}"

    def client(self, **kwargs: Any) -> txapi.AsyncTxApi:
        kwargs.setdefault("server_id", 3)
        return txapi.AsyncTxApi(api_key=KEY, base_url=self.base_url, **kwargs)

    async def read_command(self, ws: ServerConnection) -> str:
        frame = json.loads(await ws.recv())
        self.received.append(frame)
        assert frame["type"] == "command"
        return str(frame["command"])


async def send(ws: ServerConnection, **frame: Any) -> None:
    await ws.send(json.dumps(frame))


async def hold(ws: ServerConnection) -> None:
    """Keep the connection open until the client goes away."""
    try:
        await ws.wait_closed()
    except Exception:
        pass


async def collect(
    stream: ConsoleStream | ServerLogStream,
    until: Callable[[list[Any]], bool],
    timeout: float = 5.0,
) -> list[Any]:
    events: list[Any] = []

    async def run() -> None:
        async for event in stream:
            events.append(event)
            if until(events):
                return

    await asyncio.wait_for(run(), timeout)
    return events


def count(kind: type) -> Callable[[list[Any]], bool]:
    def check(events: list[Any]) -> bool:
        return sum(isinstance(e, kind) for e in events) >= 1

    return check


async def wait_connected(stream: ConsoleStream | ServerLogStream) -> None:
    for _ in range(500):
        if stream.connected:
            return
        await asyncio.sleep(0.01)
    raise AssertionError("stream did not connect")


# ─────────────────────────────────────────────────────────────── frames


async def test_init_and_log_lines_and_query_auth() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await send(ws, type="init", lines=["a", "b"])
        await send(ws, type="log", lines=["c", "d"])
        await hold(ws)

    async with LiveServer(behaviour) as server:
        client = server.client()
        async with client.console.connect() as stream:
            events = await collect(stream, lambda ev: sum(isinstance(e, LogEvent) for e in ev) == 2)
        assert isinstance(events[0], OpenEvent)
        assert events[0].reconnected is False
        assert events[1] == InitEvent(["a", "b"])
        assert events[2:] == [LogEvent("c"), LogEvent("d")]
        assert server.paths == [f"/ws/console?apiKey={KEY}&serverId=3"]
        await client.close()


async def test_serverlog_entries_objects_and_strings() -> None:
    entry = {"ts": 1, "type": "playerJoining", "src": {"name": "John"}}

    async def behaviour(ws: ServerConnection, n: int) -> None:
        await send(ws, type="init", entries=[entry, "plain line"])
        await send(ws, type="log", entries=[{"ts": 2, "type": "ChatMessage"}])
        await hold(ws)

    async with LiveServer(behaviour) as server:
        client = server.client(server_id=None)
        async with client.serverlog.connect(server_id=8) as stream:
            events = await collect(stream, count(LogEvent))
        assert events[1] == InitEvent([entry, "plain line"])
        assert events[2] == LogEvent({"ts": 2, "type": "ChatMessage"})
        assert server.paths[0].startswith("/ws/serverlog?apiKey=")
        assert server.paths[0].endswith("&serverId=8")


async def test_raw_frames_and_on_event_callback() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await send(ws, type="log", lines=["x"])
        await hold(ws)

    seen: list[Any] = []

    async def on_event(event: Any) -> None:
        seen.append(event)

    async with LiveServer(behaviour) as server:
        client = server.client()
        async with client.console.connect(raw_frames=True, on_event=on_event) as stream:
            events = await collect(stream, count(LogEvent))
    frames = [e for e in events if isinstance(e, FrameEvent)]
    assert frames == [FrameEvent({"type": "log", "lines": ["x"]})]
    assert LogEvent("x") in seen


async def test_sync_client_hands_out_streams() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await send(ws, type="init", lines=["hello"])
        await hold(ws)

    async with LiveServer(behaviour) as server:
        client = txapi.TxApi(api_key=KEY, base_url=server.base_url, server_id=1)
        async with client.console.connect() as stream:
            events = await collect(stream, count(InitEvent))
        assert events[-1] == InitEvent(["hello"])
        client.close()


# ─────────────────────────────────────────────────────────────── commands


async def test_send_command_ack_error_and_fifo() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        assert await server.read_command(ws) == "status"
        await send(ws, type="ack", message="Command sent")
        assert await server.read_command(ws) == "bad"
        await send(ws, type="error", message="txAdmin is unreachable")
        first = await server.read_command(ws)
        second = await server.read_command(ws)
        await send(ws, type="ack", message=f"ack {first}")
        await send(ws, type="ack", message=f"ack {second}")
        await hold(ws)

    async with LiveServer(behaviour) as server:
        client = server.client()
        async with client.console.connect() as stream:
            await wait_connected(stream)
            assert await stream.send_command("status") == "Command sent"
            with pytest.raises(txapi.CommandError, match="unreachable"):
                await stream.send_command("bad")
            one, two = await asyncio.gather(stream.send_command("one"), stream.send_command("two"))
            assert (one, two) == ("ack one", "ack two")
        assert server.received[0] == {"type": "command", "command": "status"}


async def test_send_command_timeout_keeps_fifo_slot() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await server.read_command(ws)  # never answered in time
        await server.read_command(ws)
        await send(ws, type="ack", message="late ack for the first")
        await send(ws, type="ack", message="ack for the second")
        await hold(ws)

    async with LiveServer(behaviour) as server:
        client = server.client()
        async with client.console.connect() as stream:
            await wait_connected(stream)
            with pytest.raises(txapi.APITimeoutError):
                await stream.send_command("slow", timeout=0.1)
            assert await stream.send_command("next") == "ack for the second"


async def test_default_command_timeout_option() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await server.read_command(ws)
        await hold(ws)

    async with LiveServer(behaviour) as server:
        client = server.client()
        async with client.console.connect(command_timeout=0.05) as stream:
            await wait_connected(stream)
            with pytest.raises(txapi.APITimeoutError):
                await stream.send_command("x")


async def test_send_command_validation_and_not_connected() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await hold(ws)

    async with LiveServer(behaviour) as server:
        client = server.client()
        stream = client.console.connect()
        with pytest.raises(txapi.APIConnectionError):
            await stream.send_command("status")
        async with stream:
            await wait_connected(stream)
            with pytest.raises(txapi.ValidationError):
                await stream.send_command("   ")
            with pytest.raises(txapi.ValidationError, match="1024"):
                await stream.send_command("x" * 1025)
    assert server.received == []


async def test_error_frame_without_pending_command_is_an_event() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await send(ws, type="error", message="Too many commands")
        await hold(ws)

    async with LiveServer(behaviour) as server, server.client().console.connect() as stream:
        events = await collect(stream, count(ErrorEvent))
    assert events[-1] == ErrorEvent("Too many commands")


async def test_pending_command_fails_when_connection_drops() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        if n == 0:
            await server.read_command(ws)
            await ws.close(1011, "txAdmin connection lost")
        else:
            await hold(ws)

    async with LiveServer(behaviour) as server:
        async with server.client().console.connect(**FAST) as stream:
            await wait_connected(stream)
            with pytest.raises(txapi.APIConnectionError):
                await stream.send_command("status")


# ─────────────────────────────────────────────────────────────── reconnect


@pytest.mark.parametrize("code", [1001, 1011, 1012])
async def test_reconnects_after_recoverable_close(code: int) -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await send(ws, type="init", lines=[f"buffer {n}"])
        if n == 0:
            await ws.close(code, "server restarting")
        else:
            await hold(ws)

    async with LiveServer(behaviour) as server:
        async with server.client().console.connect(**FAST) as stream:
            events = await collect(
                stream, lambda ev: sum(isinstance(e, InitEvent) for e in ev) == 2
            )
    kinds = [type(e).__name__ for e in events]
    assert kinds == [
        "OpenEvent",
        "InitEvent",
        "CloseEvent",
        "ReconnectingEvent",
        "OpenEvent",
        "InitEvent",
    ]
    close = events[2]
    assert close == CloseEvent(code, "server restarting", True)
    reconnecting = events[3]
    assert isinstance(reconnecting, ReconnectingEvent)
    assert reconnecting.attempt == 1
    assert reconnecting.code == code
    assert events[4] == OpenEvent(reconnected=True)
    assert events[5] == InitEvent(["buffer 1"])  # full buffer again after reconnect
    assert len(server.paths) == 2


async def test_reconnects_after_abnormal_closure_1006() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        if n == 0:
            ws.transport.abort()
            return
        await send(ws, type="init", lines=["back"])
        await hold(ws)

    async with LiveServer(behaviour) as server:
        async with server.client().console.connect(**FAST) as stream:
            events = await collect(stream, count(InitEvent))
    closes = [e for e in events if isinstance(e, CloseEvent)]
    assert closes[0].code == 1006
    assert closes[0].will_reconnect
    assert events[-1] == InitEvent(["back"])


@pytest.mark.parametrize("code", [1008, 1009])
async def test_no_reconnect_on_policy_close_and_reason_is_surfaced(code: int) -> None:
    reason = "API key lacks permission console.write"

    async def behaviour(ws: ServerConnection, n: int) -> None:
        await ws.close(code, reason)

    async with LiveServer(behaviour) as server:
        stream = server.client().console.connect(**FAST)
        events = await collect(
            stream, lambda ev: False if not ev else isinstance(ev[-1], CloseEvent)
        )
        # Iteration ends by itself after the final close.
        rest = [e async for e in stream]
        await stream.close()
    assert events[-1] == CloseEvent(code, reason, False)
    assert rest == []
    assert stream.close_code == code
    assert stream.close_reason == reason
    assert len(server.paths) == 1


async def test_reconnect_disabled() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await ws.close(1011, "boom")

    async with LiveServer(behaviour) as server:
        stream = server.client().console.connect(reconnect=False)
        events = [e async for e in stream]
        await stream.close()
    assert events[-1] == CloseEvent(1011, "boom", False)
    assert len(server.paths) == 1


async def test_user_close_stops_without_reconnect() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:
        await send(ws, type="init", lines=["x"])
        await hold(ws)

    async with LiveServer(behaviour) as server:
        stream = server.client().console.connect(**FAST)
        async with stream:
            await collect(stream, count(InitEvent))
        rest = [e async for e in stream]
        await asyncio.wait_for(server.closed.wait(), 5)
        await asyncio.sleep(0.1)
    assert rest[-1] == CloseEvent(1000, "closed by client", False, by_user=True)
    assert not any(isinstance(e, ReconnectingEvent) for e in rest)
    assert len(server.paths) == 1
    assert not stream.connected


async def test_handshake_rejection_is_final_and_hides_the_key() -> None:
    async def behaviour(ws: ServerConnection, n: int) -> None:  # pragma: no cover
        await hold(ws)

    async with LiveServer(behaviour, reject_status=403) as server:
        stream = server.client().console.connect(**FAST)
        events = [e async for e in stream]
        await stream.close()
    assert len(events) == 1
    close = events[0]
    assert isinstance(close, CloseEvent)
    assert close.code is None
    assert not close.will_reconnect
    assert "403" in close.reason
    assert KEY not in close.reason
    assert len(server.paths) == 1


def _free_port() -> int:
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return int(sock.getsockname()[1])


async def test_connection_refused_gives_up_after_max_attempts() -> None:
    client = txapi.AsyncTxApi(api_key=KEY, base_url=f"http://127.0.0.1:{_free_port()}")
    stream = client.console.connect(max_reconnect_attempts=2, **FAST)
    events = await asyncio.wait_for(_drain(stream), 10)
    await stream.close()
    kinds = [type(e).__name__ for e in events]
    assert kinds == ["ReconnectingEvent", "ReconnectingEvent", "CloseEvent"]
    assert [e.attempt for e in events[:2]] == [1, 2]
    assert events[-1].will_reconnect is False
    assert KEY not in events[-1].reason


async def _drain(stream: ConsoleStream) -> list[Any]:
    return [e async for e in stream]


def test_reconnect_delay_is_bounded() -> None:
    from txapi.live import LiveConfig

    stream = ConsoleStream(
        LiveConfig("https://x", KEY, None, "ua"), initial_reconnect_delay=1, max_reconnect_delay=30
    )
    for attempt in range(1, 12):
        delay = stream._delay(attempt)
        expected = min(30.0, 2.0 ** (attempt - 1))
        assert expected / 2 <= delay <= expected
    assert stream._url() == f"wss://x/ws/console?apiKey={KEY}"
    assert KEY not in repr(stream)
