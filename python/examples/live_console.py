"""Stream the live console and run a command (needs ``pip install "txapi-sdk[live]"``).

export TXAPI_KEY=txp_...
python examples/live_console.py --server 1 --command "status"
"""

from __future__ import annotations

import argparse
import asyncio

from txapi import AsyncTxApi, CommandError, TxApiError
from txapi.live import CloseEvent, ErrorEvent, InitEvent, LogEvent, ReconnectingEvent


async def main(server_id: int | None, command: str | None) -> None:
    async with AsyncTxApi(server_id=server_id) as client, client.console.connect() as console:
        async for event in console:
            if isinstance(event, InitEvent):
                # Sent on every (re)connect with the full buffer.
                print(f"--- buffer: {len(event.items)} lines ---")
                if command:
                    try:
                        ack = await console.send_command(command)
                        print(f"--- {ack} ---")
                    except CommandError as exc:
                        print(f"--- command failed: {exc.message} ---")
                    command = None  # only once
            elif isinstance(event, LogEvent):
                print(event.item)
            elif isinstance(event, ErrorEvent):
                print(f"--- error: {event.message} ---")
            elif isinstance(event, ReconnectingEvent):
                print(f"--- reconnecting in {event.delay:.1f} s (attempt {event.attempt}) ---")
            elif isinstance(event, CloseEvent) and not event.will_reconnect:
                # 1008 = auth/permission/quota: the reason says what to fix.
                print(f"--- closed ({event.code}): {event.reason} ---")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="txApi live console")
    parser.add_argument("--server", type=int, default=None)
    parser.add_argument("--command", default=None)
    args = parser.parse_args()
    try:
        asyncio.run(main(args.server, args.command))
    except KeyboardInterrupt:
        pass
    except TxApiError as exc:
        print(f"Error: {exc}")
