"""Structural types the generated resource classes depend on."""

from __future__ import annotations

from collections.abc import Mapping
from typing import Any, Protocol

from ._core import ApiResponse, EndpointSpec
from .live import LiveConfig

__all__ = ["AsyncRequester", "SyncRequester"]


class SyncRequester(Protocol):
    def _request(
        self,
        endpoint: EndpointSpec,
        *,
        path: Mapping[str, object],
        query: Mapping[str, object],
        body: Mapping[str, object] | None,
        server_id: int | str | None,
        timeout: float | None,
    ) -> ApiResponse[Any]: ...

    def _live_config(self, server_id: int | str | None) -> LiveConfig: ...


class AsyncRequester(Protocol):
    async def _request(
        self,
        endpoint: EndpointSpec,
        *,
        path: Mapping[str, object],
        query: Mapping[str, object],
        body: Mapping[str, object] | None,
        server_id: int | str | None,
        timeout: float | None,
    ) -> ApiResponse[Any]: ...

    def _live_config(self, server_id: int | str | None) -> LiveConfig: ...
