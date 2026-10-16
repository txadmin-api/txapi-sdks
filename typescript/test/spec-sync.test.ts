import { describe, expect, it } from "vitest";
import { API_VERSION, WEBHOOK_EVENTS } from "../src";
import { ENDPOINTS } from "../src/endpoints";
import { loadManifest } from "./helpers";

describe("spec sync", () => {
  const manifest = loadManifest();

  it("targets the manifest's API version", () => {
    expect(API_VERSION).toBe(manifest.apiVersion);
  });

  it("internal endpoint table mirrors spec/endpoints.json", () => {
    const ids = Object.keys(ENDPOINTS).sort();
    expect(ids).toEqual(manifest.endpoints.map((e) => e.id).sort());
    for (const e of manifest.endpoints) {
      const def = ENDPOINTS[e.id as keyof typeof ENDPOINTS];
      expect(def.method, e.id).toBe(e.method);
      expect(def.path, e.id).toBe(e.path);
      expect(def.permission, e.id).toBe(e.permission);
      expect(def.accountLevel, e.id).toBe(!!e.accountLevel);
      expect([...def.requiredParams], e.id).toEqual((e.params ?? []).filter((p) => p.required).map((p) => p.name));
      expect(
        def.body.map((b) => ({ name: b.name, type: b.type, required: b.required, maxLength: "maxLength" in b ? b.maxLength : undefined })),
        e.id,
      ).toEqual((e.body ?? []).map((b) => ({ name: b.name, type: b.type, required: b.required, maxLength: b.maxLength })));
    }
  });

  it("exports every webhook event name", () => {
    expect([...WEBHOOK_EVENTS].sort()).toEqual([...manifest.webhookEvents].sort());
  });
});
