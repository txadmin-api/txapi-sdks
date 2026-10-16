import { describe, expect, it } from "vitest";
import { WEBHOOK_EVENTS, WebhookVerificationError, verifyWebhook, type WebhookPayload } from "../src";
import { loadManifest, loadVectors } from "./helpers";

const v = loadVectors();
const opts = { now: v.nowSeconds, toleranceSeconds: v.toleranceSeconds };

describe("verifyWebhook – shared vectors", () => {
  it("accepts the valid vector and returns the parsed payload", async () => {
    const payload = (await verifyWebhook(v.body, v.valid.header, v.secret, opts)) as WebhookPayload<"player.banned">;
    expect(payload.event).toBe("player.banned");
    expect(payload.serverId).toBe(1);
    expect(payload.data).toEqual({
      request: {
        identifier: "license:0123456789abcdef0123456789abcdef01234567",
        reason: "cheating",
        durationHours: 24,
      },
    });
  });

  it("accepts any of several v1 signatures (rotated secret)", async () => {
    await expect(verifyWebhook(v.body, v.validWithRotatedSecret.header, v.secret, opts)).resolves.toMatchObject({
      event: "player.banned",
    });
  });

  it("accepts raw bytes (Uint8Array / ArrayBuffer)", async () => {
    const bytes = new TextEncoder().encode(v.body);
    await expect(verifyWebhook(bytes, v.valid.header, v.secret, opts)).resolves.toBeTruthy();
    await expect(verifyWebhook(bytes.buffer.slice(0), v.valid.header, v.secret, opts)).resolves.toBeTruthy();
  });

  it("accepts `now` as a Date and uses the default tolerance of 300 s", async () => {
    await expect(verifyWebhook(v.body, v.valid.header, v.secret, { now: new Date(v.nowSeconds * 1000) })).resolves.toBeTruthy();
  });

  for (const c of v.invalid) {
    it(`rejects: ${c.name}`, async () => {
      const err = await verifyWebhook(c.body ?? v.body, c.header, v.secret, opts).catch((e: unknown) => e);
      expect(err).toBeInstanceOf(WebhookVerificationError);
      expect((err as Error).message).not.toContain(v.secret);
    });
  }

  it("rejects a wrong secret", async () => {
    await expect(verifyWebhook(v.body, v.valid.header, "whsec_other", opts)).rejects.toBeInstanceOf(WebhookVerificationError);
  });

  it("rejects a missing header", async () => {
    await expect(verifyWebhook(v.body, null as unknown as string, v.secret, opts)).rejects.toBeInstanceOf(
      WebhookVerificationError,
    );
  });

  it("rejects an already-parsed object (the raw body is required)", async () => {
    await expect(
      verifyWebhook(JSON.parse(v.body) as unknown as string, v.valid.header, v.secret, opts),
    ).rejects.toBeInstanceOf(WebhookVerificationError);
  });

  it("a re-serialised body fails (whitespace matters)", async () => {
    const reserialised = JSON.stringify(JSON.parse(v.body), null, 2);
    await expect(verifyWebhook(reserialised, v.valid.header, v.secret, opts)).rejects.toBeInstanceOf(
      WebhookVerificationError,
    );
  });
});

describe("WEBHOOK_EVENTS", () => {
  it("matches the spec manifest", () => {
    expect([...WEBHOOK_EVENTS].sort()).toEqual([...loadManifest().webhookEvents].sort());
  });
});
