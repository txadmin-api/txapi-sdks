"""SPEC §8: webhook signature verification against spec/webhook-vectors.json."""

from __future__ import annotations

import hashlib
import hmac
import json
from typing import Any

import pytest

import txapi
from txapi import webhooks

from .conftest import VECTORS_PATH, load_spec

VECTORS: dict[str, Any] = json.loads(VECTORS_PATH.read_text(encoding="utf-8"))
SECRET: str = VECTORS["secret"]
NOW: int = VECTORS["nowSeconds"]
BODY: str = VECTORS["body"]


def _verify(header: str, body: str | bytes = BODY, **kwargs: Any) -> txapi.WebhookEvent:
    kwargs.setdefault("now", NOW)
    kwargs.setdefault("tolerance_seconds", VECTORS["toleranceSeconds"])
    return txapi.verify_webhook(body, header, SECRET, **kwargs)


def test_valid_vector() -> None:
    event = _verify(VECTORS["valid"]["header"])
    assert event["event"] == "player.banned"
    assert event["serverId"] == 1
    assert event["data"]["request"]["durationHours"] == 24


def test_valid_vector_with_bytes_body() -> None:
    assert _verify(VECTORS["valid"]["header"], BODY.encode("utf-8"))["id"].startswith("5f0c")


def test_rotated_secret_vector() -> None:
    assert _verify(VECTORS["validWithRotatedSecret"]["header"])["event"] == "player.banned"


@pytest.mark.parametrize("case", VECTORS["invalid"], ids=lambda c: c["name"])
def test_invalid_vectors(case: dict[str, Any]) -> None:
    with pytest.raises(txapi.WebhookVerificationError) as info:
        _verify(case["header"], case.get("body", BODY))
    assert info.value.reason
    assert SECRET not in str(info.value)


@pytest.mark.parametrize(
    ("name", "reason"),
    [
        ("wrong signature", "signature_mismatch"),
        ("body tampered", "signature_mismatch"),
        ("too old", "timestamp_out_of_tolerance"),
        ("too far in the future", "timestamp_out_of_tolerance"),
        ("missing timestamp", "missing_timestamp"),
        ("missing signature", "missing_signature"),
        ("garbage", "missing_timestamp"),
        ("empty", "missing_timestamp"),
    ],
)
def test_invalid_reasons(name: str, reason: str) -> None:
    case = next(c for c in VECTORS["invalid"] if c["name"] == name)
    with pytest.raises(txapi.WebhookVerificationError) as info:
        _verify(case["header"], case.get("body", BODY))
    assert info.value.reason == reason


def test_tolerance_is_configurable() -> None:
    old = next(c for c in VECTORS["invalid"] if c["name"] == "too old")
    assert _verify(old["header"], tolerance_seconds=2000)["event"] == "player.banned"


def test_reserialised_json_breaks_the_signature() -> None:
    reserialised = json.dumps(json.loads(BODY), indent=2)
    with pytest.raises(txapi.WebhookVerificationError):
        _verify(VECTORS["valid"]["header"], reserialised)


def test_none_header_and_missing_secret() -> None:
    with pytest.raises(txapi.WebhookVerificationError):
        txapi.verify_webhook(BODY, None, SECRET, now=NOW)
    with pytest.raises(txapi.WebhookVerificationError) as info:
        txapi.verify_webhook(BODY, VECTORS["valid"]["header"], "", now=NOW)
    assert info.value.reason == "missing_secret"


def test_non_numeric_timestamp() -> None:
    with pytest.raises(txapi.WebhookVerificationError) as info:
        _verify("t=abc,v1=00")
    assert info.value.reason == "malformed_header"


def test_non_ascii_signature_does_not_crash() -> None:
    with pytest.raises(txapi.WebhookVerificationError) as info:
        _verify(f"t={NOW},v1=äöü")
    assert info.value.reason == "signature_mismatch"


def test_signed_non_json_body() -> None:
    body = b"not json"
    signature = hmac.new(SECRET.encode(), f"{NOW}.".encode() + body, hashlib.sha256).hexdigest()
    with pytest.raises(txapi.WebhookVerificationError) as info:
        txapi.verify_webhook(body, f"t={NOW},v1={signature}", SECRET, now=NOW)
    assert info.value.reason == "invalid_body"


def test_event_names_match_the_spec() -> None:
    assert set(txapi.WEBHOOK_EVENTS) == set(load_spec()["webhookEvents"])
    assert len(txapi.WEBHOOK_EVENTS) == len(set(txapi.WEBHOOK_EVENTS))
    assert webhooks.SIGNATURE_HEADER == "X-Txapi-Signature"
    assert webhooks.EVENT_HEADER == "X-Txapi-Event"
    assert webhooks.DELIVERY_HEADER == "X-Txapi-Delivery"


def test_event_name_literal_matches_the_tuple() -> None:
    from typing import get_args

    assert set(get_args(txapi.WebhookEventName)) == set(txapi.WEBHOOK_EVENTS)
