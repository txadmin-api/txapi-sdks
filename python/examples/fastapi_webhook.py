"""Receive txApi webhooks with FastAPI.

    pip install txapi-sdk fastapi uvicorn
    export TXAPI_WEBHOOK_SECRET=whsec_...
    uvicorn examples.fastapi_webhook:app --port 8000

The signature covers the raw request body. Read it with ``await request.body()``
— never verify ``json.dumps(await request.json())``: re-serialising changes the bytes.
"""

from __future__ import annotations

import os
from typing import Optional

from fastapi import FastAPI, Header, HTTPException, Request

from txapi import WebhookVerificationError, verify_webhook
from txapi.webhooks import SIGNATURE_HEADER

app = FastAPI()
SECRET = os.environ.get("TXAPI_WEBHOOK_SECRET", "")


@app.post("/txapi/webhook")
async def txapi_webhook(
    request: Request,
    # Optional[...] (not ``str | None``): FastAPI evaluates annotations at runtime (3.9).
    x_txapi_signature: Optional[str] = Header(default=None, alias=SIGNATURE_HEADER),  # noqa: UP045
) -> dict[str, bool]:
    raw = await request.body()  # raw bytes, exactly as sent
    try:
        event = verify_webhook(raw, x_txapi_signature, SECRET)
    except WebhookVerificationError as exc:
        raise HTTPException(status_code=400, detail=exc.reason) from None

    if event["event"] == "player.banned":
        print("Ban on server", event.get("serverId"), event["data"].get("request"))
    elif event["event"] == "server.offline":
        print("Server went offline:", event.get("serverId"))
    else:
        print("Event", event["event"], event["id"])
    # Answer 2xx quickly; failed deliveries are retried (1 min … 6 h).
    return {"ok": True}
