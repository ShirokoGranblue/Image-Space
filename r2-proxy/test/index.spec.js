import { describe, it, expect, vi, beforeEach } from "vitest";
import worker from "../src/index.js";

let testId = 0;

describe("R2 media proxy", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    testId += 1;
  });

  it("serves public images after authoritative metadata validation", async () => {
    const storageKey = uniqueKey("public-hit.png");
    const backendFetch = vi.fn(async () => jsonResponse(publicMeta(storageKey, 7)));
    const env = envWithObject({ backendFetch });

    const response = await worker.fetch(
      new Request(`https://cdn.image-space.app/public/${storageKey}?v=7`),
      env,
      {},
    );

    expect(response.status).toBe(200);
    expect(response.headers.get("Cache-Control")).toBe("public, max-age=31536000, immutable");
    expect(await response.text()).toBe("image-bytes");
    expect(backendFetch).toHaveBeenCalledOnce();
  });

  it("returns 502 when authoritative metadata lookup fails", async () => {
    const storageKey = uniqueKey("backend-fallback.png");
    const backendFetch = vi.fn(async () => new Response(null, { status: 503 }));
    const env = envWithObject({ backendFetch });

    const response = await worker.fetch(
      new Request(`https://cdn.image-space.app/public/${storageKey}?v=2`),
      env,
      {},
    );

    expect(response.status).toBe(502);
    expect(backendFetch).toHaveBeenCalledOnce();
  });

  it("revalidates public cache hits without reading R2 twice", async () => {
    const storageKey = uniqueKey("cached.png");
    const backendFetch = vi.fn(async () => jsonResponse(publicMeta(storageKey, 1)));
    const getObject = vi.fn(async () => objectBody("cached-bytes"));
    const env = envWithObject({ backendFetch, getObject });
    const request = new Request(`https://cdn.image-space.app/public/${storageKey}?v=1`);

    expect((await worker.fetch(request, env, {})).status).toBe(200);
    expect((await worker.fetch(request, env, {})).status).toBe(200);

    expect(backendFetch).toHaveBeenCalledTimes(2);
    expect(getObject).toHaveBeenCalledOnce();
  });

  it("revalidates cached public responses against authoritative backend metadata", async () => {
    const storageKey = uniqueKey("revoked.png");
    const backendFetch = vi.fn()
      .mockResolvedValueOnce(jsonResponse(publicMeta(storageKey, 1)))
      .mockResolvedValueOnce(jsonResponse(privateMeta(storageKey, 1)));
    const getObject = vi.fn(async () => objectBody("cached-bytes"));
    const env = envWithObject({ backendFetch, getObject });
    const request = new Request(`https://cdn.image-space.app/public/${storageKey}?v=1`);

    expect((await worker.fetch(request, env, {})).status).toBe(200);
    const revokedResponse = await worker.fetch(request, env, {});

    expect(revokedResponse.status).toBe(404);
    expect(backendFetch).toHaveBeenCalledTimes(2);
    expect(getObject).toHaveBeenCalledOnce();
  });

  it("returns 404 when public URLs point at private metadata", async () => {
    const storageKey = uniqueKey("private-on-public.png");
    const backendFetch = vi.fn(async () => jsonResponse(privateMeta(storageKey, 3)));
    const env = envWithObject({ backendFetch });

    const response = await worker.fetch(
      new Request(`https://cdn.image-space.app/public/${storageKey}?v=3`),
      env,
      {},
    );

    expect(response.status).toBe(404);
    expect(response.headers.get("Cache-Control")).toBe("no-store");
    expect(env.MY_BUCKET.get).not.toHaveBeenCalled();
  });

  it("returns 404 for stale public URL versions", async () => {
    const storageKey = uniqueKey("stale.png");
    const backendFetch = vi.fn(async () => jsonResponse(publicMeta(storageKey, 9)));
    const env = envWithObject({ backendFetch });

    const response = await worker.fetch(
      new Request(`https://cdn.image-space.app/public/${storageKey}?v=8`),
      env,
      {},
    );

    expect(response.status).toBe(404);
    expect(env.MY_BUCKET.get).not.toHaveBeenCalled();
  });

  it("serves private images only after backend authorization", async () => {
    const storageKey = uniqueKey("private-ok.png");
    const authFetch = vi.fn(async () => new Response(null, { status: 204 }));
    const env = envWithObject({ authFetch });

    const response = await worker.fetch(
      new Request(`https://cdn.image-space.app/private/${storageKey}?auth=token&expires=1893456000`),
      env,
      {},
    );

    expect(response.status).toBe(200);
    expect(response.headers.get("Cache-Control")).toBe("no-store");
    expect(authFetch).toHaveBeenCalledOnce();
    const authUrl = new URL(authFetch.mock.calls[0][0].url);
    expect(authUrl.searchParams.get("storageKey")).toBe(storageKey);
    expect(authUrl.searchParams.get("token")).toBe("token");
  });

  it("blocks private images without credentials", async () => {
    const storageKey = uniqueKey("private-missing-token.png");
    const authFetch = vi.fn();
    const env = envWithObject({ authFetch });

    const response = await worker.fetch(
      new Request(`https://cdn.image-space.app/private/${storageKey}`),
      env,
      {},
    );

    expect(response.status).toBe(403);
    expect(authFetch).not.toHaveBeenCalled();
  });

  it("rejects legacy root CDN paths", async () => {
    const env = envWithObject();
    const response = await worker.fetch(
      new Request("https://cdn.image-space.app/images/legacy.png?v=1"),
      env,
      {},
    );

    expect(response.status).toBe(404);
    expect(env.MY_BUCKET.get).not.toHaveBeenCalled();
  });

  it("rejects non-gallery storage keys", async () => {
    const env = envWithObject();
    const response = await worker.fetch(
      new Request("https://cdn.image-space.app/public/avatars/a.png?v=1"),
      env,
      {},
    );

    expect(response.status).toBe(404);
    expect(env.MY_BUCKET.get).not.toHaveBeenCalled();
  });
});

function envWithObject({ backendFetch, authFetch, getObject } = {}) {
  return {
    BACKEND_META_URL: "https://image-space.app/api/internal/media/meta",
    BACKEND_AUTHORIZE_URL: "https://image-space.app/api/internal/media/authorize",
    BACKEND_INTERNAL_TOKEN: "internal-secret",
    BACKEND_META: backendFetch ? { fetch: backendFetch } : undefined,
    BACKEND_AUTH: authFetch ? { fetch: authFetch } : undefined,
    MY_BUCKET: {
      get: getObject || vi.fn(async () => objectBody("image-bytes")),
    },
  };
}

function objectBody(body) {
  return {
    body,
    httpEtag: "\"etag\"",
    writeHttpMetadata(headers) {
      headers.set("Content-Type", "image/png");
    },
  };
}

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function publicMeta(storageKey, version) {
  return {
    imageId: 42,
    storageKey,
    visibility: "public",
    ownerId: 4,
    version,
  };
}

function privateMeta(storageKey, version) {
  return {
    ...publicMeta(storageKey, version),
    visibility: "private",
  };
}

function uniqueKey(name) {
  return `images/test-${testId}-${name}`;
}
