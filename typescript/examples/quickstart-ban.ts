/**
 * Quickstart: find a player and ban them for 24 hours.
 *
 *   npm run build
 *   TXAPI_KEY=txp_… TXAPI_SERVER_ID=1 node --experimental-strip-types examples/quickstart-ban.ts "john" "cheating"
 *
 * (or `npx tsx examples/quickstart-ban.ts …`)
 */
import { NotFoundError, RateLimitError, TxApi, TxApiError } from "@txapi/sdk";

const [query = "john", reason = "Cheating (automated test)"] = process.argv.slice(2);

const txapi = new TxApi({
  // apiKey defaults to process.env.TXAPI_KEY
  serverId: process.env.TXAPI_SERVER_ID,
});

try {
  const me = await txapi.account.me();
  console.log(`Plan ${me.plan?.code}, ${me.quota?.remaining ?? "∞"} requests left this month`);

  const { players } = await txapi.players.search({ query, filters: ["isOnline"] });
  const target = players[0];
  if (!target) {
    console.log(`No online player matches "${query}".`);
    process.exit(0);
  }
  console.log(`Banning ${target.displayName} (${target.license}) …`);

  const result = await txapi.players.ban({
    identifier: `license:${target.license}`,
    reason,
    durationHours: 24,
  });
  console.log("Done:", result.message ?? result);
  console.log("Rate limit:", txapi.lastRateLimit);
} catch (err) {
  if (err instanceof RateLimitError && err.quotaExhausted) {
    console.error("Monthly quota used up — it resets on the 1st (UTC).");
  } else if (err instanceof NotFoundError) {
    console.error("Not found:", err.message);
  } else if (err instanceof TxApiError) {
    console.error(`${err.name} (${err.status ?? "no response"}): ${err.message}`);
  } else {
    throw err;
  }
  process.exitCode = 1;
}
