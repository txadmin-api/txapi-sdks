/**
 * Live console: print every console line and run a command.
 *
 *   npm run build
 *   TXAPI_KEY=txp_… TXAPI_SERVER_ID=1 node --experimental-strip-types examples/live-console.ts "status"
 *
 * Node ≥ 22 has a global WebSocket. On Node 18/20: `npm i ws` and pass it
 * (`new TxApi({ WebSocket })` with `import { WebSocket } from "ws"`).
 */
import { CommandRejectedError, StreamClosedError, TxApi } from "@txapi/sdk";

const command = process.argv[2] ?? "status";
const txapi = new TxApi({ serverId: process.env.TXAPI_SERVER_ID });

const live = txapi.console.connect();
live.on("init", (lines) => console.log(`--- ${lines.length} buffered lines ---`));
live.on("log", (line) => console.log(line));
live.on("reconnecting", ({ attempt, delayMs, code }) =>
  console.warn(`connection lost (${code}), reconnect #${attempt} in ${delayMs} ms`),
);
live.on("error", (err) => console.warn("server says:", err.message));

try {
  await live.ready(); // first `init` frame received
  console.log(`> ${command}`, "→", await live.sendCommand(command));
} catch (err) {
  if (err instanceof StreamClosedError) {
    // 1008 = bad key, missing console.write, quota or too slow — no reconnect.
    console.error(`stream closed (${err.code}): ${err.reason}`);
  } else if (err instanceof CommandRejectedError) {
    console.error("command rejected:", err.message);
  } else {
    console.error(err);
  }
  process.exitCode = 1;
}

// Keep streaming for a few seconds, then stop.
setTimeout(() => void live.close(), 5_000);
const info = await live.closed();
console.log(`closed (${info.code})`);
