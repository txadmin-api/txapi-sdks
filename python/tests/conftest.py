from __future__ import annotations

import json
import re
from collections.abc import Callable
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import httpx
import pytest

import txapi

SPEC_PATH = Path(__file__).resolve().parents[2] / "spec" / "endpoints.json"
VECTORS_PATH = Path(__file__).resolve().parents[2] / "spec" / "webhook-vectors.json"
KEY = "txp_live_SECRETKEY_do_not_leak_1234567890"
BASE = "https://api.test"


def load_spec() -> dict[str, Any]:
    spec: dict[str, Any] = json.loads(SPEC_PATH.read_text(encoding="utf-8"))
    return spec


Handler = Callable[[httpx.Request], httpx.Response]


@dataclass
class Recorder:
    """Recording mock transport: every request is stored, answers come from ``handler``."""

    handler: Handler | None = None
    requests: list[httpx.Request] = field(default_factory=list)

    def __call__(self, request: httpx.Request) -> httpx.Response:
        request.read()
        self.requests.append(request)
        if self.handler is not None:
            return self.handler(request)
        return httpx.Response(200, json={"success": True, "message": "ok"})

    @property
    def last(self) -> httpx.Request:
        return self.requests[-1]

    def sync_client(self, **kwargs: Any) -> txapi.TxApi:
        kwargs.setdefault("api_key", KEY)
        kwargs.setdefault("base_url", BASE)
        return txapi.TxApi(http_client=httpx.Client(transport=httpx.MockTransport(self)), **kwargs)

    def async_client(self, **kwargs: Any) -> txapi.AsyncTxApi:
        kwargs.setdefault("api_key", KEY)
        kwargs.setdefault("base_url", BASE)
        return txapi.AsyncTxApi(
            http_client=httpx.AsyncClient(transport=httpx.MockTransport(self)), **kwargs
        )


@pytest.fixture
def recorder() -> Recorder:
    return Recorder()


@pytest.fixture(autouse=True)
def _clean_env(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("TXAPI_KEY", raising=False)
    monkeypatch.delenv("TXAPI_BASE_URL", raising=False)


@pytest.fixture
def sleeps(monkeypatch: pytest.MonkeyPatch) -> list[float]:
    """Replace the retry sleeps (sync and async) with a recorder."""
    recorded: list[float] = []

    def fake_sleep(seconds: float) -> None:
        recorded.append(seconds)

    async def fake_async_sleep(seconds: float) -> None:
        recorded.append(seconds)

    monkeypatch.setattr(txapi._client, "_sleep", fake_sleep)
    monkeypatch.setattr(txapi._client, "_async_sleep", fake_async_sleep)
    return recorded


def template_regexes(spec: dict[str, Any]) -> list[tuple[str, str, re.Pattern[str]]]:
    """(method, template, regex) — literal templates first so they win over ``{param}`` ones."""
    out = []
    for ep in spec["endpoints"]:
        pattern = "^" + re.sub(r"\\\{[^}]+\\\}", "[^/]+", re.escape(ep["path"])) + "$"
        out.append((ep["method"], ep["path"], re.compile(pattern)))
    out.sort(key=lambda item: "{" in item[1])
    return out


def match_template(
    regexes: list[tuple[str, str, re.Pattern[str]]], method: str, raw_path: str
) -> str | None:
    for m, template, regex in regexes:
        if m == method and regex.match(raw_path):
            return f"{m} {template}"
    return None
