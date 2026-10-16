/**
 * Minimal webhook receiver on node:http — no framework needed.
 *
 *   npm run build
 *   TXAPI_WEBHOOK_SECRET=whsec_… node --experimental-strip-types examples/webhook-server.ts
 *
 * Point a webhook (dashboard → Webhooks) at http(s)://<host>:3000/txapi and press "Send test".
 * IMPORTANT: verify the RAW body bytes — never JSON.parse + JSON.stringify first.
 */
import { createServer } from "node:http";
import { SIGNATURE_HEADER, WebhookVerificationError, verifyWebhook } from "@txapi/sdk";

const secret = process.env.TXAPI_WEBHOOK_SECRET;
if (!secret) throw new Error("Set TXAPI_WEBHOOK_SECRET");

const seen = new Set<string>(); // use a database in production

const server = createServer(async (req, res) => {
  if (req.method !== "POST" || req.url !== "/txapi") {
    res.writeHead(404).end();
    return;
  }
  const chunks: Buffer[] = [];
  for await (const chunk of req) chunks.push(chunk as Buffer);
  const rawBody = Buffer.concat(chunks);

  try {
    const event = await verifyWebhook(rawBody, req.headers[SIGNATURE_HEADER.toLowerCase()] as string, secret);
    if (seen.has(event.id)) {
      res.writeHead(200).end("duplicate");
      return;
    }
    seen.add(event.id);
    console.log(`${event.event} on server ${event.serverId ?? "-"}:`, JSON.stringify(event.data));
    res.writeHead(200).end("ok");
  } catch (err) {
    if (err instanceof WebhookVerificationError) {
      console.warn("rejected delivery:", err.message);
      res.writeHead(400).end("invalid signature");
      return;
    }
    res.writeHead(500).end();
  }
});

server.listen(Number(process.env.PORT ?? 3000), () => console.log("listening on :3000/txapi"));
