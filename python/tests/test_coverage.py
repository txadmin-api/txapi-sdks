"""SPEC §3 coverage test: every endpoint of spec/endpoints.json has exactly one method,
on the sync and async client and on both ``with_raw_response`` variants."""

from __future__ import annotations

import inspect
from collections.abc import Iterator
from typing import Any

import pytest

import txapi
from txapi import _generated

from .conftest import Recorder, load_spec, match_template, template_regexes

GROUPS = (
    "account",
    "players",
    "history",
    "server",
    "console",
    "serverlog",
    "events",
    "whitelist",
    "admins",
)


def _endpoint_methods(group: object, prefix: str) -> Iterator[tuple[str, Any]]:
    """Every endpoint method of a group (recursing into nested groups)."""
    for name in sorted(dir(group)):
        if name.startswith("_"):
            continue
        value = getattr(group, name)
        if inspect.ismethod(value):
            if name.startswith("iter_") or name == "connect":
                continue
            yield prefix + name, value
        elif type(value).__module__ == _generated.__name__:
            yield from _endpoint_methods(value, f"{prefix}{name}.")


def _arguments(method: Any) -> tuple[list[Any], dict[str, Any]]:
    args: list[Any] = []
    kwargs: dict[str, Any] = {}
    for param in inspect.signature(method).parameters.values():
        if param.default is not inspect.Parameter.empty:
            continue
        annotation = str(param.annotation)
        if annotation == "bool":
            value: Any = True
        elif annotation == "int":
            value = 1
        elif "Sequence" in annotation or annotation.startswith("list"):
            value = ["players.read"]
        else:
            value = "license:abc"
        if param.kind is inspect.Parameter.POSITIONAL_OR_KEYWORD:
            args.append(value)
        else:
            kwargs[param.name] = value
    return args, kwargs


def _roots(client: Any) -> list[tuple[str, object]]:
    return [(g, getattr(client, g)) for g in GROUPS]


def _record_templates(recorder: Recorder) -> list[str]:
    regexes = template_regexes(load_spec())
    out = []
    for request in recorder.requests:
        raw_path = request.url.raw_path.split(b"?", 1)[0].decode("ascii")
        matched = match_template(regexes, request.method, raw_path)
        assert matched is not None, f"unexpected request {request.method} {raw_path}"
        out.append(matched)
    return out


def _expected() -> set[str]:
    return {f"{ep['method']} {ep['path']}" for ep in load_spec()["endpoints"]}


def test_spec_has_67_endpoints() -> None:
    spec = load_spec()
    assert len(spec["endpoints"]) == 67
    assert spec["apiVersion"] == txapi.API_VERSION


@pytest.mark.parametrize("raw", [False, True], ids=["parsed", "with_raw_response"])
def test_sync_client_covers_every_endpoint(recorder: Recorder, raw: bool) -> None:
    client = recorder.sync_client(server_id=7)
    target = client.with_raw_response if raw else client
    names = []
    for group_name, group in _roots(target):
        for name, method in _endpoint_methods(group, group_name + "."):
            names.append(name)
            args, kwargs = _arguments(method)
            result = method(*args, **kwargs)
            if raw:
                assert isinstance(result, txapi.ApiResponse)
                assert result.status_code == 200
    recorded = _record_templates(recorder)
    assert len(names) == 67, names
    assert len(recorded) == 67
    assert set(recorded) == _expected()


@pytest.mark.parametrize("raw", [False, True], ids=["parsed", "with_raw_response"])
async def test_async_client_covers_every_endpoint(recorder: Recorder, raw: bool) -> None:
    client = recorder.async_client(server_id=7)
    target = client.with_raw_response if raw else client
    names = []
    for group_name, group in _roots(target):
        for name, method in _endpoint_methods(group, group_name + "."):
            names.append(name)
            args, kwargs = _arguments(method)
            result = await method(*args, **kwargs)
            if raw:
                assert isinstance(result, txapi.ApiResponse)
    recorded = _record_templates(recorder)
    assert len(names) == 67, names
    assert set(recorded) == _expected()
    await client.close()


def test_sync_and_async_surfaces_are_identical() -> None:
    sync = txapi.TxApi(api_key="k")
    asynchronous = txapi.AsyncTxApi(api_key="k")

    def surface(client: Any) -> dict[str, list[str]]:
        out: dict[str, list[str]] = {}
        for group_name, group in _roots(client):
            stack: list[tuple[str, object]] = [(group_name, group)]
            while stack:
                prefix, obj = stack.pop()
                for name in dir(obj):
                    if name.startswith("_"):
                        continue
                    value = getattr(obj, name)
                    if inspect.ismethod(value):
                        params = list(inspect.signature(value).parameters)
                        out[f"{prefix}.{name}"] = params
                    elif type(value).__module__ == _generated.__name__:
                        stack.append((f"{prefix}.{name}", value))
        return out

    assert surface(sync) == surface(asynchronous)
    assert surface(sync.with_raw_response).keys() <= surface(sync).keys()
    assert surface(asynchronous.with_raw_response) == surface(sync.with_raw_response)


def test_named_methods_from_the_spec_exist() -> None:
    client = txapi.TxApi(api_key="k")
    for path in (
        "players.ban",
        "history.revoke",
        "server.resources.restart",
        "whitelist.approvals.add",
        "events.admin_actions",
        "account.me",
        "console.connect",
        "serverlog.connect",
    ):
        obj: Any = client
        for part in path.split("."):
            obj = getattr(obj, part)
        assert callable(obj), path
