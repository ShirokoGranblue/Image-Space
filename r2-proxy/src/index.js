const PUBLIC_CACHE_CONTROL = "public, max-age=0, s-maxage=604800, must-revalidate";
const PRIVATE_CACHE_CONTROL = "private, no-store";

export default {
  async fetch(request, env, ctx) {
    if (request.method !== "GET" && request.method !== "HEAD") {
      return textResponse("Method Not Allowed", 405, PRIVATE_CACHE_CONTROL);
    }

    const url = new URL(request.url);
    const objectKey = decodeURIComponent(url.pathname.replace(/^\/+/, ""));
    if (!objectKey) {
      return textResponse("Missing file key", 400, PRIVATE_CACHE_CONTROL);
    }

    const accessToken = url.searchParams.get("auth");
    if (accessToken) {
      return handlePrivateRequest(request, env, objectKey, accessToken);
    }

    return handlePublicRequest(request, env, ctx, objectKey);
  },
};

async function handlePublicRequest(request, env, ctx, objectKey) {
  if (!hasAllowedReferer(request, env)) {
    return textResponse("Forbidden", 403, PRIVATE_CACHE_CONTROL);
  }

  const head = await getObjectHead(env, objectKey);
  if (!head) {
    return textResponse("Not Found", 404, PRIVATE_CACHE_CONTROL);
  }
  if (head.customMetadata?.public !== "true") {
    return textResponse("Forbidden", 403, PRIVATE_CACHE_CONTROL);
  }

  const cache = caches.default;
  const cacheKey = publicCacheKey(request);
  if (request.method === "GET") {
    const cached = await cache.match(cacheKey);
    if (cached) {
      return cached;
    }
  }

  const response = await serveFromR2(request, env, objectKey, PUBLIC_CACHE_CONTROL);
  if (response.ok && request.method === "GET") {
    const cachePut = cache.put(cacheKey, response.clone());
    if (typeof ctx?.waitUntil === "function") {
      ctx.waitUntil(cachePut);
    } else {
      await cachePut;
    }
  }
  return response;
}

async function handlePrivateRequest(request, env, objectKey, accessToken) {
  const allowed = await authorizeWithBackend(env, objectKey, accessToken);
  if (!allowed) {
    return textResponse("Forbidden", 403, PRIVATE_CACHE_CONTROL);
  }
  return serveFromR2(request, env, objectKey, PRIVATE_CACHE_CONTROL);
}

async function authorizeWithBackend(env, objectKey, accessToken) {
  const endpoint = env.BACKEND_AUTH_URL || "https://image-space.app/api/image/media/authorize";
  const url = new URL(endpoint);
  url.searchParams.set("key", objectKey);
  url.searchParams.set("token", accessToken);

  const authRequest = new Request(url.toString(), {
    method: "GET",
    headers: { "Accept": "application/json" },
  });

  const response = env.BACKEND_AUTH?.fetch
    ? await env.BACKEND_AUTH.fetch(authRequest)
    : await fetch(authRequest);

  return response.status >= 200 && response.status < 300;
}

async function getObjectHead(env, objectKey) {
  try {
    return await env.MY_BUCKET.head(objectKey);
  } catch {
    return null;
  }
}

async function serveFromR2(request, env, objectKey, cacheControl) {
  try {
    const object = await env.MY_BUCKET.get(objectKey);
    if (!object) {
      return textResponse("Not Found", 404, PRIVATE_CACHE_CONTROL);
    }

    const headers = headersForObject(object, cacheControl);
    return new Response(request.method === "HEAD" ? null : object.body, { headers });
  } catch {
    return textResponse("Internal Server Error", 500, PRIVATE_CACHE_CONTROL);
  }
}

function headersForObject(object, cacheControl) {
  const headers = new Headers();
  object.writeHttpMetadata?.(headers);
  if (object.httpEtag) {
    headers.set("etag", object.httpEtag);
  }
  headers.set("Cache-Control", cacheControl);
  headers.set("X-Content-Type-Options", "nosniff");
  return headers;
}

function publicCacheKey(request) {
  const url = new URL(request.url);
  url.searchParams.delete("auth");
  url.searchParams.delete("expires");
  return new Request(url.toString(), { method: "GET" });
}

function hasAllowedReferer(request, env) {
  const referer = request.headers.get("Referer");
  if (!referer) {
    return true;
  }

  let host;
  try {
    host = new URL(referer).hostname.toLowerCase();
  } catch {
    return false;
  }

  const allowed = (env.ALLOWED_REFERERS || "image-space.app,*.image-space.app,localhost,127.0.0.1")
    .split(",")
    .map((value) => value.trim().toLowerCase())
    .filter(Boolean);

  return allowed.some((pattern) => matchesHostPattern(host, pattern));
}

function matchesHostPattern(host, pattern) {
  if (pattern.startsWith("*.")) {
    const suffix = pattern.slice(1);
    return host.endsWith(suffix) && host.length > suffix.length;
  }
  return host === pattern;
}

function textResponse(message, status, cacheControl) {
  return new Response(message, {
    status,
    headers: {
      "Cache-Control": cacheControl,
      "X-Content-Type-Options": "nosniff",
    },
  });
}
