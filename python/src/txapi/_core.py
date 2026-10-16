"""Transport-independent core: endpoint specs, request building (SPEC §2),
response/error mapping (SPEC §4), retry decisions (SPEC §5) and configuration (SPEC §1)."""

from __future__ import annotations

import json
import os
import random
from collections.abc import Callable, Mapping
from dataclasses import dataclass, field
from typing import Any, Generic, TypeVar
from urllib.parse import quote

import httpx

from ._errors import RateLimitError, TxApiError, ValidationError, error_class_for_status
from ._ratelimit import RateLimit, parse_rate_limit, parse_retry_after
from ._version import __version__

__all__ = ["ApiResponse"]

T = TypeVar("T")

DEFAULT_BASE_URL = "https://txapi.cloud"
DEFAULT_TIMEOUT = 30.0
DEFAULT_MAX_RETRIES = 2
MAX_RETRY_AFTER = 60.0
"""A ``Retry-After`` above this is not waited for (SPEC §5)."""
RETRYABLE_GET_STATUSES = frozenset({502, 503, 504})


@dataclass(frozen=True)
class FieldSpec:
    """A query parameter, path parameter or JSON body field of an endpoint."""

    name: str
    """Name on the wire (camelCase)."""
    py_name: str
    """Python keyword argument name (snake_case), used in validation messages."""
    required: bool = False
    type: str = "string"
    max_length: int | None = None


@dataclass(frozen=True)
class EndpointSpec:
    """One endpoint of ``spec/endpoints.json``."""

    id: str
    method: str
    path: str
    permission: str | None
    account_level: bool
    path_params: tuple[FieldSpec, ...] = ()
    query: tuple[FieldSpec, ...] = ()
    body: tuple[FieldSpec, ...] = ()


@dataclass
class ApiResponse(Generic[T]):
    """Parsed data together with the raw HTTP response (SPEC §6).

    Returned by every method of ``client.with_raw_response``.
    """

    data: T
    status_code: int
    headers: httpx.Headers
    rate_limit: RateLimit | None
    http_response: httpx.Response = field(repr=False)


@dataclass(frozen=True)
class PreparedRequest:
    method: str
    path: str
    params: list[tuple[str, str]]
    body: bytes | None


# ─────────────────────────────────────────────────────────────── configuration


@dataclass(frozen=True)
class ClientConfig:
    api_key: str = field(repr=False)
    base_url: str
    server_id: int | str | None
    timeout: float
    max_retries: int
    user_agent: str


def resolve_config(
    api_key: str | None,
    base_url: str | None,
    server_id: int | str | None,
    timeout: float | None,
    max_retries: int | None,
    user_agent: str | None,
) -> ClientConfig:
    key = api_key if api_key is not None else os.environ.get("TXAPI_KEY")
    if key is None or not key.strip():
        raise ValidationError(
            "No API key: pass api_key=... or set the TXAPI_KEY environment variable."
        )
    url = base_url or os.environ.get("TXAPI_BASE_URL") or DEFAULT_BASE_URL
    url = url.strip().rstrip("/")
    if not url.startswith(("http://", "https://")):
        raise ValidationError("base_url must start with http:// or https://")
    retries = DEFAULT_MAX_RETRIES if max_retries is None else max_retries
    if retries < 0:
        raise ValidationError("max_retries must be >= 0")
    agent = f"txapi-python/{__version__}"
    if user_agent:
        agent = f"{agent} {user_agent}"
    return ClientConfig(
        api_key=key.strip(),
        base_url=url,
        server_id=server_id,
        timeout=DEFAULT_TIMEOUT if timeout is None else float(timeout),
        max_retries=retries,
        user_agent=agent,
    )


def default_headers(config: ClientConfig) -> dict[str, str]:
    return {
        "Authorization": f"Bearer {config.api_key}",
        "Accept": "application/json",
        "User-Agent": config.user_agent,
    }


# ───────────────────────────────────────────────────────────── request building


def _is_blank(value: object) -> bool:
    if isinstance(value, str):
        return not value.strip()
    if isinstance(value, (list, tuple)):
        return len(value) == 0
    return False


def _fail(endpoint: EndpointSpec, message: str) -> ValidationError:
    return ValidationError(f"{endpoint.id}: {message}")


def _query_value(value: object) -> str:
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (list, tuple)):
        return ",".join(_query_value(v) for v in value)
    return str(value)


def _check_length(endpoint: EndpointSpec, spec: FieldSpec, value: object) -> None:
    if spec.max_length is not None and isinstance(value, str) and len(value) > spec.max_length:
        raise _fail(
            endpoint,
            f"'{spec.py_name}' is {len(value)} characters long, "
            f"the API accepts at most {spec.max_length}",
        )


def build_request(
    endpoint: EndpointSpec,
    *,
    path: Mapping[str, object],
    query: Mapping[str, object],
    body: Mapping[str, object] | None,
    server_id: int | str | None,
    default_server_id: int | str | None,
) -> PreparedRequest:
    """Validate and assemble a request (SPEC §2). Raises :class:`ValidationError`."""
    rendered = endpoint.path
    for spec in endpoint.path_params:
        value = path.get(spec.name)
        if value is None or _is_blank(value):
            raise _fail(endpoint, f"'{spec.py_name}' is required")
        rendered = rendered.replace("{" + spec.name + "}", quote(str(value), safe=""))

    params: list[tuple[str, str]] = []
    for spec in endpoint.query:
        value = query.get(spec.name)
        if value is None:
            if spec.required:
                raise _fail(endpoint, f"'{spec.py_name}' is required")
            continue
        if spec.required and _is_blank(value):
            raise _fail(endpoint, f"'{spec.py_name}' must not be blank")
        params.append((spec.name, _query_value(value)))

    if not endpoint.account_level:
        sid = server_id if server_id is not None else default_server_id
        if sid is not None:
            params.append(("serverId", str(sid)))

    payload: bytes | None = None
    if endpoint.body:
        document: dict[str, object] = {}
        for spec in endpoint.body:
            value = (body or {}).get(spec.name)
            if value is None:
                if spec.required:
                    raise _fail(endpoint, f"'{spec.py_name}' is required")
                continue
            if spec.required and _is_blank(value):
                raise _fail(endpoint, f"'{spec.py_name}' must not be blank")
            _check_length(endpoint, spec, value)
            document[spec.name] = list(value) if isinstance(value, tuple) else value
        payload = json.dumps(document, separators=(",", ":")).encode("utf-8")

    return PreparedRequest(method=endpoint.method, path=rendered, params=params, body=payload)


# ─────────────────────────────────────────────────────────────── responses


def parse_body(response: httpx.Response) -> Any:
    """JSON when possible, otherwise the text (``None`` for an empty body)."""
    content = response.content
    if not content or not content.strip():
        return None
    try:
        return json.loads(content)
    except ValueError:
        return response.text


def error_message(response: httpx.Response, body: Any) -> str:
    if isinstance(body, dict):
        for key in ("error", "message"):
            value = body.get(key)
            if isinstance(value, str) and value.strip():
                return value
    if isinstance(body, str) and body.strip():
        text = body.strip()
        return text if len(text) <= 500 else text[:500] + "…"
    if body is not None and not isinstance(body, str):
        return json.dumps(body)[:500]
    return response.reason_phrase or f"HTTP {response.status_code}"


def error_from_response(response: httpx.Response, body: Any) -> TxApiError:
    """Map an error response to the matching exception class (SPEC §4)."""
    status = response.status_code
    message = error_message(response, body)
    headers = response.headers
    rate_limit = parse_rate_limit(headers)
    cls = error_class_for_status(status)
    if cls is RateLimitError:
        return RateLimitError(
            message,
            retry_after=parse_retry_after(headers),
            status=status,
            body=body,
            headers=headers,
            rate_limit=rate_limit,
        )
    return cls(message, status=status, body=body, headers=headers, rate_limit=rate_limit)


# ─────────────────────────────────────────────────────────────── retries


def backoff_delay(attempt: int, rand: Callable[[], float] = random.random) -> float:
    """Full jitter: ``uniform(0, min(8, 0.5 * 2**attempt))`` (SPEC §5)."""
    return rand() * min(8.0, 0.5 * (2.0**attempt))


def retry_delay_for_response(method: str, response: httpx.Response, attempt: int) -> float | None:
    """Seconds to wait before retrying this response, or ``None`` if it must not be retried."""
    status = response.status_code
    retry_after = parse_retry_after(response.headers)
    if status == 429:
        # Without Retry-After the monthly quota is exhausted: never retry.
        if retry_after is None or retry_after > MAX_RETRY_AFTER:
            return None
        return retry_after
    if status in RETRYABLE_GET_STATUSES and method == "GET":
        if retry_after is not None and retry_after <= MAX_RETRY_AFTER:
            return retry_after
        return backoff_delay(attempt)
    return None


def retry_delay_for_transport_error(method: str, attempt: int) -> float | None:
    if method != "GET":
        return None
    return backoff_delay(attempt)


def make_api_response(response: httpx.Response, data: T) -> ApiResponse[T]:
    return ApiResponse(
        data=data,
        status_code=response.status_code,
        headers=response.headers,
        rate_limit=parse_rate_limit(response.headers),
        http_response=response,
    )
