const PUBLIC_CACHE_CONTROL = "public, max-age=31536000, immutable";
const NO_STORE_CACHE_CONTROL = "no-store";

const PUBLIC_PREFIX = "/public/";
const PRIVATE_PREFIX = "/private/";
const STORAGE_KEY_PREFIX = "images/";

export default {
  async fetch(request, env, ctx) {
    if (request.method !== "GET" && request.method !== "HEAD") {
      return textResponse("Method Not Allowed", 405, NO_STORE_CACHE_CONTROL);
    }

    const route = parseRoute(request);
    if (!route.ok) {
      logEvent("media_route_rejected", { reason: route.reason });
      return textResponse(route.message, route.status, NO_STORE_CACHE_CONTROL);
    }

    if (route.type === "public") {
      return handlePublicRequest(request, env, ctx, route.storageKey);
    }
    return handlePrivateRequest(request, env, route.storageKey);
  },
};

async function handlePublicRequest(request, env, ctx, storageKey) {
  const cache = caches.default;
  const cacheKey = new Request(request.url, { method: "GET" });

  if (request.method === "GET") {
    const cached = await cache.match(cacheKey);
    if (cached) {
      logEvent("media_public_cache_hit", { storageKey });
      return cached;
    }
  }

  let meta;
  try {
    meta = await getMediaMeta(env, storageKey);
  } catch (error) {
    logEvent("media_meta_lookup_failed", { storageKey, reason: error.message });
    return textResponse("Bad Gateway", 502, NO_STORE_CACHE_CONTROL);
  }

  if (!meta || normalizeVisibility(meta.visibility) !== "public") {
    logEvent("media_public_not_found", { storageKey, hasMeta: Boolean(meta) });
    return textResponse("Not Found", 404, NO_STORE_CACHE_CONTROL);
  }

  const url = new URL(request.url);
  const requestedVersion = url.searchParams.get("v");
  if (!requestedVersion || requestedVersion !== String(meta.version)) {
    logEvent("media_public_version_mismatch", { storageKey });
    return textResponse("Not Found", 404, NO_STORE_CACHE_CONTROL);
  }

  const response = await serveFromR2(request, env, storageKey, PUBLIC_CACHE_CONTROL);
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

async function handlePrivateRequest(request, env, storageKey) {
  const authToken = new URL(request.url).searchParams.get("auth");
  const authorization = request.headers.get("Authorization");
  const saToken = request.headers.get("satoken");

  if (!authToken && !authorization && !saToken) {
    logEvent("media_private_missing_credentials", { storageKey });
    return textResponse("Forbidden", 403, NO_STORE_CACHE_CONTROL);
  }

  let allowed;
  try {
    allowed = await authorizeWithBackend(env, storageKey, authToken, authorization, saToken);
  } catch (error) {
    logEvent("media_private_authorize_failed", { storageKey, reason: error.message });
    return textResponse("Bad Gateway", 502, NO_STORE_CACHE_CONTROL);
  }

  if (!allowed) {
    logEvent("media_private_forbidden", { storageKey });
    return textResponse("Forbidden", 403, NO_STORE_CACHE_CONTROL);
  }
  return serveFromR2(request, env, storageKey, NO_STORE_CACHE_CONTROL);
}

async function getMediaMeta(env, storageKey) {
  const redisKey = metaCacheKey(storageKey);
  if (hasUpstash(env)) {
    const cached = await getMetaFromRedis(env, redisKey);
    if (cached) {
      return cached;
    }
  }

  const meta = await getMetaFromBackend(env, storageKey);
  if (meta && hasUpstash(env)) {
    await putMetaToRedis(env, redisKey, meta).catch((error) => {
      logEvent("media_meta_redis_put_failed", { storageKey, reason: error.message });
    });
  }
  return meta;
}

async function getMetaFromRedis(env, redisKey) {
  const value = await upstashCommand(env, ["GET", redisKey]);
  if (value == null) {
    return null;
  }
  if (typeof value === "object") {
    return normalizeMeta(value);
  }
  try {
    return normalizeMeta(JSON.parse(value));
  } catch (error) {
    logEvent("media_meta_redis_parse_failed", { redisKey, reason: error.message });
    return null;
  }
}

async function putMetaToRedis(env, redisKey, meta) {
  const ttl = Number(env.MEDIA_META_TTL_SECONDS || 300);
  await upstashCommand(env, ["SET", redisKey, JSON.stringify(meta), "EX", String(ttl)]);
}

async function upstashCommand(env, command) {
  const response = await fetch(env.UPSTASH_REDIS_REST_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${env.UPSTASH_REDIS_REST_TOKEN}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(command),
  });
  if (!response.ok) {
    throw new Error(`redis_${response.status}`);
  }
  const body = await response.json();
  if (body.error) {
    throw new Error("redis_command_error");
  }
  return body.result;
}

async function getMetaFromBackend(env, storageKey) {
  const endpoint = env.BACKEND_META_URL || "https://image-space.app/api/internal/media/meta";
  const url = new URL(endpoint);
  url.searchParams.set("storageKey", storageKey);

  const response = await fetchWithOptionalBinding(env, "BACKEND_META", url.toString(), {
    method: "GET",
    headers: internalHeaders(env),
  });

  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`backend_meta_${response.status}`);
  }
  return normalizeMeta(await response.json());
}

async function authorizeWithBackend(env, storageKey, authToken, authorization, saToken) {
  const endpoint = env.BACKEND_AUTHORIZE_URL || "https://image-space.app/api/internal/media/authorize";
  const url = new URL(endpoint);
  url.searchParams.set("storageKey", storageKey);
  if (authToken) {
    url.searchParams.set("token", authToken);
  }

  const headers = internalHeaders(env);
  if (authorization) {
    headers.set("Authorization", authorization);
  }
  if (saToken) {
    headers.set("satoken", saToken);
  }

  const response = await fetchWithOptionalBinding(env, "BACKEND_AUTH", url.toString(), {
    method: "GET",
    headers,
  });
  return response.status >= 200 && response.status < 300;
}

async function fetchWithOptionalBinding(env, bindingName, url, init) {
  const request = new Request(url, init);
  const binding = env[bindingName];
  return binding?.fetch ? binding.fetch(request) : fetch(request);
}

async function serveFromR2(request, env, storageKey, cacheControl) {
  try {
    const object = await env.MY_BUCKET.get(storageKey);
    if (!object) {
      logEvent("media_r2_not_found", { storageKey });
      return textResponse("Not Found", 404, NO_STORE_CACHE_CONTROL);
    }

    const headers = headersForObject(object, cacheControl);
    return new Response(request.method === "HEAD" ? null : object.body, { headers });
  } catch (error) {
    logEvent("media_r2_failed", { storageKey, reason: error.message });
    return textResponse("Bad Gateway", 502, NO_STORE_CACHE_CONTROL);
  }
}

function parseRoute(request) {
  let path;
  try {
    path = decodeURIComponent(new URL(request.url).pathname);
  } catch {
    return badRoute("Malformed path", 400, "malformed_path");
  }

  let type;
  let storageKey;
  if (path.startsWith(PUBLIC_PREFIX)) {
    type = "public";
    storageKey = path.slice(PUBLIC_PREFIX.length);
  } else if (path.startsWith(PRIVATE_PREFIX)) {
    type = "private";
    storageKey = path.slice(PRIVATE_PREFIX.length);
  } else {
    return badRoute("Not Found", 404, "legacy_or_unknown_path");
  }

  storageKey = normalizeStorageKey(storageKey);
  if (!storageKey || !storageKey.startsWith(STORAGE_KEY_PREFIX)) {
    return badRoute("Not Found", 404, "unsupported_storage_key");
  }
  return { ok: true, type, storageKey };
}

function normalizeStorageKey(value) {
  const key = (value || "").replace(/^\/+/, "");
  if (!key || key.includes("..") || key.includes("\\")) {
    return "";
  }
  return key;
}

function normalizeMeta(meta) {
  if (!meta || typeof meta !== "object") {
    return null;
  }
  return {
    imageId: meta.imageId,
    storageKey: meta.storageKey,
    visibility: normalizeVisibility(meta.visibility),
    ownerId: meta.ownerId,
    version: meta.version,
  };
}

function normalizeVisibility(value) {
  const normalized = String(value || "").toLowerCase();
  return normalized === "public" ? "public" : "private";
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

function internalHeaders(env) {
  const headers = new Headers({ Accept: "application/json" });
  if (env.BACKEND_INTERNAL_TOKEN) {
    headers.set("X-Internal-Token", env.BACKEND_INTERNAL_TOKEN);
  }
  return headers;
}

function hasUpstash(env) {
  return Boolean(env.UPSTASH_REDIS_REST_URL && env.UPSTASH_REDIS_REST_TOKEN);
}

function metaCacheKey(storageKey) {
  return `media:meta:${storageKey}`;
}

function badRoute(message, status, reason) {
  return { ok: false, message, status, reason };
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

function logEvent(event, details) {
  console.log(JSON.stringify({ event, ...details }));
}
