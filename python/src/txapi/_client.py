"""The synchronous :class:`TxApi` and asynchronous :class:`AsyncTxApi` clients."""

from __future__ import annotations

import asyncio
import inspect
import logging
import time
from collections.abc import Callable, Mapping
from types import TracebackType
from typing import Any

import httpx

from . import _core
from ._core import ApiResponse, ClientConfig, EndpointSpec, PreparedRequest
from ._errors import APIConnectionError, APITimeoutError, TxApiError
from ._generated import AsyncGroups, AsyncTxApiWithRawResponse, SyncGroups, TxApiWithRawResponse
from ._ratelimit import RateLimit, parse_rate_limit
from .live import LiveConfig

__all__ = ["AsyncTxApi", "TxApi"]

logger = logging.getLogger("txapi")

RequestHook = Callable[[httpx.Request], Any]
"""Called before every attempt. Never log ``request.headers`` — they contain the key."""
ResponseHook = Callable[[httpx.Response], Any]
"""Called after every attempt that produced a response (also for errors and retries)."""

# Module-level indirections so tests can replace the sleeps.
_sleep: Callable[[float], None] = time.sleep


async def _async_sleep(seconds: float) -> None:
    await asyncio.sleep(seconds)


class _ClientBase:
    _config: ClientConfig
    _on_request: RequestHook | None
    _on_response: ResponseHook | None
    last_rate_limit: RateLimit | None
    """Rate-limit state of the most recent response (SPEC §6), ``None`` before the first."""

    def _init_base(
        self,
        api_key: str | None,
        base_url: str | None,
        server_id: int | str | None,
        timeout: float | None,
        max_retries: int | None,
        user_agent: str | None,
        on_request: RequestHook | None,
        on_response: ResponseHook | None,
    ) -> None:
        self._config = _core.resolve_config(
            api_key, base_url, server_id, timeout, max_retries, user_agent
        )
        self._on_request = on_request
        self._on_response = on_response
        self.last_rate_limit = None

    @property
    def base_url(self) -> str:
        """Base URL without ``/api/v1`` (no trailing slash)."""
        return self._config.base_url

    @property
    def server_id(self) -> int | str | None:
        """Default server for server-scoped calls."""
        return self._config.server_id

    @property
    def timeout(self) -> float:
        """Default per-attempt timeout in seconds."""
        return self._config.timeout

    @property
    def max_retries(self) -> int:
        """Retries after the first attempt (SPEC §5)."""
        return self._config.max_retries

    def __repr__(self) -> str:
        return (
            f"{type(self).__name__}(base_url={self._config.base_url!r}, "
            f"server_id={self._config.server_id!r}, api_key=<redacted>)"
        )

    def _live_config(self, server_id: int | str | None) -> LiveConfig:
        return LiveConfig(
            base_url=self._config.base_url,
            api_key=self._config.api_key,
            server_id=server_id if server_id is not None else self._config.server_id,
            user_agent=self._config.user_agent,
        )

    def _prepare(
        self,
        endpoint: EndpointSpec,
        path: Mapping[str, object],
        query: Mapping[str, object],
        body: Mapping[str, object] | None,
        server_id: int | str | None,
    ) -> PreparedRequest:
        return _core.build_request(
            endpoint,
            path=path,
            query=query,
            body=body,
            server_id=server_id,
            default_server_id=self._config.server_id,
        )

    def _http_request(
        self,
        http: httpx.Client | httpx.AsyncClient,
        prepared: PreparedRequest,
        timeout: float | None,
    ) -> httpx.Request:
        headers = _core.default_headers(self._config)
        if prepared.body is not None:
            headers["Content-Type"] = "application/json"
        return http.build_request(
            prepared.method,
            self._config.base_url + prepared.path,
            params=tuple(prepared.params),
            content=prepared.body,
            headers=headers,
            timeout=httpx.Timeout(self._config.timeout if timeout is None else timeout),
        )

    def _record(self, response: httpx.Response) -> None:
        rate = parse_rate_limit(response.headers)
        if rate is not None:
            self.last_rate_limit = rate

    def _transport_error(self, exc: httpx.TransportError) -> TxApiError:
        name = type(exc).__name__
        if isinstance(exc, httpx.TimeoutException):
            return APITimeoutError(f"request timed out ({name})")
        return APIConnectionError(f"connection failed ({name}: {exc})")


def _call_hook(hook: Callable[[Any], Any] | None, value: Any) -> Any:
    if hook is None:
        return None
    try:
        return hook(value)
    except Exception:
        logger.exception("txapi: hook raised; ignored")
        return None


class TxApi(_ClientBase, SyncGroups):
    """Synchronous txApi client.

    Example::

        from txapi import TxApi

        with TxApi(server_id=1) as client:  # key from TXAPI_KEY
            page = client.players.search(query="john")
            client.players.ban(identifier="license:" + page["players"][0]["license"],
                               reason="Cheating", duration_hours=168)

    Args:
        api_key: API key; defaults to the ``TXAPI_KEY`` environment variable.
        base_url: Defaults to ``TXAPI_BASE_URL`` or ``https://txapi.cloud`` (``/api/v1`` is added).
        server_id: Default server for every server-scoped call (each call can override it).
        timeout: Per-attempt timeout in seconds (default 30).
        max_retries: Retries after the first attempt (default 2, ``0`` disables retries).
        user_agent: Appended to ``txapi-python/<version>``.
        http_client: Your own ``httpx.Client`` (proxies, transports …). It is not closed
            by :meth:`close`.
        on_request: Hook called before every attempt (logging/metrics). Exceptions are ignored.
            The request headers contain your key — never log them.
        on_response: Hook called with every response. Exceptions are ignored.
    """

    with_raw_response: TxApiWithRawResponse
    """The same methods, returning :class:`~txapi.ApiResponse` (data + status, headers,
    rate limit)."""

    def __init__(
        self,
        api_key: str | None = None,
        *,
        base_url: str | None = None,
        server_id: int | str | None = None,
        timeout: float | None = None,
        max_retries: int | None = None,
        user_agent: str | None = None,
        http_client: httpx.Client | None = None,
        on_request: RequestHook | None = None,
        on_response: ResponseHook | None = None,
    ) -> None:
        self._init_base(
            api_key, base_url, server_id, timeout, max_retries, user_agent, on_request, on_response
        )
        self._owns_http = http_client is None
        self._http = http_client if http_client is not None else httpx.Client()
        SyncGroups.__init__(self, self)
        self.with_raw_response = TxApiWithRawResponse(self)

    def close(self) -> None:
        """Close the underlying HTTP client (unless it was passed in)."""
        if self._owns_http:
            self._http.close()

    def __enter__(self) -> TxApi:
        return self

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc: BaseException | None,
        tb: TracebackType | None,
    ) -> None:
        self.close()

    def _request(
        self,
        endpoint: EndpointSpec,
        *,
        path: Mapping[str, object],
        query: Mapping[str, object],
        body: Mapping[str, object] | None,
        server_id: int | str | None,
        timeout: float | None,
    ) -> ApiResponse[Any]:
        prepared = self._prepare(endpoint, path, query, body, server_id)
        attempt = 0
        while True:
            request = self._http_request(self._http, prepared, timeout)
            _call_hook(self._on_request, request)
            try:
                response = self._http.send(request)
            except httpx.TransportError as exc:
                error = self._transport_error(exc)
                delay = _core.retry_delay_for_transport_error(prepared.method, attempt)
                if delay is not None and attempt < self._config.max_retries:
                    attempt += 1
                    _sleep(delay)
                    continue
                raise error from exc
            _call_hook(self._on_response, response)
            self._record(response)
            if response.is_success:
                return _core.make_api_response(response, _core.parse_body(response))
            delay = _core.retry_delay_for_response(prepared.method, response, attempt)
            if delay is not None and attempt < self._config.max_retries:
                attempt += 1
                response.close()
                _sleep(delay)
                continue
            raise _core.error_from_response(response, _core.parse_body(response))


class AsyncTxApi(_ClientBase, AsyncGroups):
    """Asynchronous txApi client — same surface as :class:`TxApi`, every call is awaited.

    Example::

        import asyncio
        from txapi import AsyncTxApi

        async def main() -> None:
            async with AsyncTxApi(server_id=1) as client:
                status = await client.server.status()
                async for row in client.history.iter_bans():
                    print(row["id"], row["reason"])

        asyncio.run(main())

    Takes the same arguments as :class:`TxApi`; ``http_client`` is an ``httpx.AsyncClient``.
    Hooks may be plain functions or coroutines.
    """

    with_raw_response: AsyncTxApiWithRawResponse
    """The same methods, returning :class:`~txapi.ApiResponse` (data + status, headers,
    rate limit)."""

    def __init__(
        self,
        api_key: str | None = None,
        *,
        base_url: str | None = None,
        server_id: int | str | None = None,
        timeout: float | None = None,
        max_retries: int | None = None,
        user_agent: str | None = None,
        http_client: httpx.AsyncClient | None = None,
        on_request: RequestHook | None = None,
        on_response: ResponseHook | None = None,
    ) -> None:
        self._init_base(
            api_key, base_url, server_id, timeout, max_retries, user_agent, on_request, on_response
        )
        self._owns_http = http_client is None
        self._http = http_client if http_client is not None else httpx.AsyncClient()
        AsyncGroups.__init__(self, self)
        self.with_raw_response = AsyncTxApiWithRawResponse(self)

    async def close(self) -> None:
        """Close the underlying HTTP client (unless it was passed in)."""
        if self._owns_http:
            await self._http.aclose()

    async def __aenter__(self) -> AsyncTxApi:
        return self

    async def __aexit__(
        self,
        exc_type: type[BaseException] | None,
        exc: BaseException | None,
        tb: TracebackType | None,
    ) -> None:
        await self.close()

    async def _hook(self, hook: Callable[[Any], Any] | None, value: Any) -> None:
        result = _call_hook(hook, value)
        if inspect.isawaitable(result):
            try:
                await result
            except Exception:
                logger.exception("txapi: hook raised; ignored")

    async def _request(
        self,
        endpoint: EndpointSpec,
        *,
        path: Mapping[str, object],
        query: Mapping[str, object],
        body: Mapping[str, object] | None,
        server_id: int | str | None,
        timeout: float | None,
    ) -> ApiResponse[Any]:
        prepared = self._prepare(endpoint, path, query, body, server_id)
        attempt = 0
        while True:
            request = self._http_request(self._http, prepared, timeout)
            await self._hook(self._on_request, request)
            try:
                response = await self._http.send(request)
            except httpx.TransportError as exc:
                error = self._transport_error(exc)
                delay = _core.retry_delay_for_transport_error(prepared.method, attempt)
                if delay is not None and attempt < self._config.max_retries:
                    attempt += 1
                    await _async_sleep(delay)
                    continue
                raise error from exc
            await self._hook(self._on_response, response)
            self._record(response)
            if response.is_success:
                return _core.make_api_response(response, _core.parse_body(response))
            delay = _core.retry_delay_for_response(prepared.method, response, attempt)
            if delay is not None and attempt < self._config.max_retries:
                attempt += 1
                await response.aclose()
                await _async_sleep(delay)
                continue
            raise _core.error_from_response(response, _core.parse_body(response))
