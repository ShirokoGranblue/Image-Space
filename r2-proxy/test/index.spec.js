import { describe, it, expect, vi, beforeEach } from "vitest";
import worker from "../src/index.js";

describe("R2 media proxy", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("serves public objects with allowed referers and cache headers", async () => {
    const env = envWithObject({ public: "true" });
    const response = await worker.fetch(new Request("https://cdn.image-space.app/images/a.png", {
      headers: { Referer: "https://image-space.app/home" },
    }), env, {});

    expect(response.status).toBe(200);
    expect(response.headers.get("Cache-Control")).toBe("public, max-age=0, s-maxage=604800, must-revalidate");
    expect(await response.text()).toBe("image-bytes");
  });

  it("blocks hotlinked public objects", async () => {
    const env = envWithObject({ public: "true" });
    const response = await worker.fetch(new Request("https://cdn.image-space.app/images/a.png", {
      headers: { Referer: "https://example.com/page" },
    }), env, {});

    expect(response.status).toBe(403);
    expect(response.headers.get("Cache-Control")).toBe("private, no-store");
  });

  it("blocks unsigned access when the R2 object is not public", async () => {
    const env = envWithObject({ public: "false" });
    const response = await worker.fetch(new Request("https://cdn.image-space.app/images/a.png"), env, {});

    expect(response.status).toBe(403);
  });

  it("serves private objects only after backend authorization", async () => {
    const authFetch = vi.fn(async () => new Response(null, { status: 204 }));
    const env = envWithObject({ public: "false" }, authFetch);

    const response = await worker.fetch(
      new Request("https://cdn.image-space.app/images/a.png?auth=token&expires=1893456000"),
      env,
      {},
    );

    expect(response.status).toBe(200);
    expect(response.headers.get("Cache-Control")).toBe("private, no-store");
    expect(authFetch).toHaveBeenCalledOnce();
    const authUrl = new URL(authFetch.mock.calls[0][0].url);
    expect(authUrl.searchParams.get("key")).toBe("images/a.png");
    expect(authUrl.searchParams.get("token")).toBe("token");
  });

  it("blocks private objects when backend authorization fails", async () => {
    const env = envWithObject({ public: "false" }, async () => new Response(null, { status: 403 }));
    const response = await worker.fetch(
      new Request("https://cdn.image-space.app/images/a.png?auth=bad-token"),
      env,
      {},
    );

    expect(response.status).toBe(403);
  });
});

function envWithObject(customMetadata, authFetch) {
  const object = {
    body: "image-bytes",
    httpEtag: "\"etag\"",
    writeHttpMetadata(headers) {
      headers.set("Content-Type", "image/png");
    },
  };

  return {
    ALLOWED_REFERERS: "image-space.app,*.image-space.app",
    BACKEND_AUTH_URL: "https://image-space.app/api/image/media/authorize",
    BACKEND_AUTH: authFetch ? { fetch: authFetch } : undefined,
    MY_BUCKET: {
      head: vi.fn(async () => ({ customMetadata })),
      get: vi.fn(async () => object),
    },
  };
}
