"""Find a player by name and ban them for a week.

export TXAPI_KEY=txp_...
python examples/quickstart_ban.py "John" "Cheating" --server 1
"""

from __future__ import annotations

import argparse
import sys

from txapi import NotFoundError, RateLimitError, TxAdminRejectedError, TxApi, TxApiError


def main() -> int:
    parser = argparse.ArgumentParser(description="Ban a player via txApi")
    parser.add_argument("name", help="part of the player's display name")
    parser.add_argument("reason", help="ban reason (max. 512 characters)")
    parser.add_argument("--server", type=int, default=None, help="server id (see /api/v1/servers)")
    parser.add_argument("--hours", type=int, default=168, help="duration, 0 = permanent")
    parser.add_argument("--discord", action="store_true", help="also ban on linked Discord servers")
    args = parser.parse_args()

    with TxApi(server_id=args.server) as client:  # key from TXAPI_KEY
        me = client.account.me()
        print(f"Plan {me['plan']['code']}, {me['quota'].get('remaining', '∞')} calls left")

        page = client.players.search(query=args.name)
        players = page.get("players", [])
        if not players:
            print(f"No player matching {args.name!r}")
            return 1
        player = players[0]
        print(f"Banning {player['displayName']} (license:{player['license']})")

        try:
            result = client.players.ban(
                identifier=f"license:{player['license']}",
                reason=args.reason,
                duration_hours=args.hours,
                discord_ban=args.discord or None,
            )
        except NotFoundError:
            print("Player not found")
            return 1
        except TxAdminRejectedError as exc:
            print(f"txAdmin refused: {exc.message}")
            return 1
        except RateLimitError as exc:
            if exc.quota_exhausted:
                print("Monthly quota used up — it resets on the 1st (UTC).")
            else:
                print(f"Rate limited, try again in {exc.retry_after:.0f} s")
            return 1
        except TxApiError as exc:
            print(f"Error: {exc}")
            return 1

        print(result.get("message", "done"))
        discord = result.get("discord")
        if discord:
            for guild in discord.get("guilds", []):
                print(f"  Discord {guild.get('guildName')}: {guild.get('code')}")
        if client.last_rate_limit:
            print(f"{client.last_rate_limit.remaining} requests left this minute")
    return 0


if __name__ == "__main__":
    sys.exit(main())
