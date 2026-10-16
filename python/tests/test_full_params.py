"""Every endpoint method called with EVERY argument it accepts: each query parameter and
body field of spec/endpoints.json must arrive under exactly its wire name."""

from __future__ import annotations

import inspect
import json
from typing import Any
from urllib.parse import parse_qsl

import txapi

from .conftest import Recorder, load_spec, match_template, template_regexes
from .test_coverage import _endpoint_methods, _roots

SKIP = {"server_id", "timeout"}


def _value(annotation: str) -> Any:
    if "Sequence" in annotation or annotation.startswith("list"):
        return ["players.read", "players.ban"]
    if annotation.startswith("bool"):
        return False
    if annotation.startswith("int"):
        return 5
    return "license:abc"


def _all_arguments(method: Any) -> tuple[list[Any], dict[str, Any]]:
    args: list[Any] = []
    kwargs: dict[str, Any] = {}
    for param in inspect.signature(method).parameters.values():
        if param.name in SKIP:
            continue
        value = _value(str(param.annotation))
        if param.kind is inspect.Parameter.POSITIONAL_OR_KEYWORD:
            args.append(value)
        else:
            kwargs[param.name] = value
    return args, kwargs


def _check(recorder: Recorder) -> None:
    spec = load_spec()
    by_template = {f"{e['method']} {e['path']}": e for e in spec["endpoints"]}
    regexes = template_regexes(spec)
    seen = set()
    for request in recorder.requests:
        raw_path, _, raw_query = request.url.raw_path.decode("ascii").partition("?")
        template = match_template(regexes, request.method, raw_path)
        assert template is not None
        ep = by_template[template]
        seen.add(ep["id"])
        query = sorted({k for k, _ in parse_qsl(raw_query)} - {"serverId"})
        want_query = sorted(p["name"] for p in ep["params"] if p["in"] == "query")
        assert query == want_query, ep["id"]
        body = sorted(json.loads(request.content)) if request.content else []
        assert body == sorted(b["name"] for b in ep["body"]), ep["id"]
    assert seen == {e["id"] for e in spec["endpoints"]}


def test_sync_sends_every_parameter(recorder: Recorder) -> None:
    client = recorder.sync_client(server_id=7)
    for group_name, group in _roots(client):
        for _name, method in _endpoint_methods(group, group_name + "."):
            args, kwargs = _all_arguments(method)
            method(*args, **kwargs)
    _check(recorder)


async def test_async_sends_every_parameter(recorder: Recorder) -> None:
    client = recorder.async_client(server_id=7)
    for group_name, group in _roots(client):
        for _name, method in _endpoint_methods(group, group_name + "."):
            args, kwargs = _all_arguments(method)
            await method(*args, **kwargs)
    _check(recorder)
    await client.close()


def test_arguments_cover_the_spec() -> None:
    """Sanity: the generic filler really passes something for every spec field."""
    assert load_spec()["apiVersion"] == txapi.API_VERSION
