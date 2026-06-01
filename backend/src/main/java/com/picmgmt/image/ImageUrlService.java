package com.picmgmt.image;

import com.picmgmt.cache.CacheService;
import com.picmgmt.util.MediaUrlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.picmgmt.util.MediaUrlUtil.withVersion;

/**
 * 统一的图片 URL 生成服务。
 * <p>
 * 策略：
 * <ul>
 *   <li><b>公开图片</b>：直接 CDN URL（无签名），依赖 CDN 缓存 + Referer 防盗链</li>
 *   <li><b>私有/指定图片</b>：短签名 presigned URL + Redis 缓存（30s 签名，25s Redis TTL）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ImageUrlService {

    private final CacheService cacheService;
    private final MediaUrlUtil mediaUrlUtil;

    /** 私有/指定图片访问 token 有效期：30 秒 */
    public static final Duration PRIVATE_ACCESS_EXPIRY = Duration.ofSeconds(30);

    private static final String URL_KEY_PREFIX = "media:url:";
    private static final String TOKEN_KEY_PREFIX = "media:token:";
    private static final String TOKEN_QUERY_PARAM = "auth";

    /** 私有图 URL Redis TTL：比访问 token 短 5 秒 */
    private static final Duration PRIVATE_URL_CACHE_TTL = PRIVATE_ACCESS_EXPIRY.minus(Duration.ofSeconds(5));

    /** 公开图片 R2/CDN Cache-Control 头 */
    public static final String PUBLIC_CACHE_CONTROL = "public, max-age=0, s-maxage=604800, must-revalidate";

    /** 私有图片 R2/Worker Cache-Control 头 */
    public static final String PRIVATE_CACHE_CONTROL = "private, no-store";

    // ── 公开图片：直接 CDN URL，无签名，无 Redis 缓存 ──

    /**
     * 获取公开图片的 CDN 直链。
     * <p>
     * URL 带 version token（基于 storageKey + uploadTime + visibility），
     * 权限变更时 uploadTime 变化 → token 变化 → CDN 缓存自动失效。
     */
    public String getPublicImageUrl(String storageKey, LocalDateTime uploadTime) {
        String cdnUrl = mediaUrlUtil.getPublicUrl() + "/" + storageKey.replaceAll("^/", "");
        String timePart = uploadTime != null ? uploadTime.toString() : "";
        return withVersion(cdnUrl, storageKey + timePart + ":PUBLIC");
    }

    // ── 私有图片：后端授权 token + Redis 缓存 ──

    /**
     * 获取带 Redis 缓存的 Worker 私有访问 URL（仅私有/指定图片使用）。
     * <p>
     * 缓存 key = media:url:{storageKey}，TTL 25 秒（访问 token 30 秒）。
     */
    public String getPrivateImageUrl(String storageKey) {
        String cacheKey = urlCacheKey(storageKey);
        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent() && privateUrlStillUsable(cached.get())) {
            return cached.get();
        }
        cached.ifPresent(ignored -> cacheService.evict(cacheKey));

        String url = createPrivateImageUrl(storageKey);
        cacheService.put(cacheKey, url, PRIVATE_URL_CACHE_TTL);
        return url;
    }

    /**
     * Worker 回调后端鉴权时校验短期访问 token。
     */
    public boolean authorizePrivateAccess(String storageKey, String token) {
        if (storageKey == null || storageKey.isBlank() || token == null || token.isBlank()) {
            return false;
        }
        String tokenKey = tokenCacheKey(token);
        Optional<String> tokenData = cacheService.get(tokenKey, String.class);
        if (tokenData.isEmpty()) {
            return false;
        }

        String[] parts = tokenData.get().split("\\n", 2);
        if (parts.length != 2) {
            cacheService.evict(tokenKey);
            return false;
        }

        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            cacheService.evict(tokenKey);
            return false;
        }

        if (Instant.now().getEpochSecond() > expiresAt) {
            cacheService.evict(tokenKey);
            return false;
        }

        return normalizeKey(storageKey).equals(normalizeKey(parts[0]));
    }

    // ── 缓存管理 ──

    /**
     * 清除指定 storageKey 的 URL 缓存（权限变更时调用）。
     */
    public void evictUrlCache(String storageKey) {
        if (storageKey != null && !storageKey.isBlank()) {
            cacheService.evict(urlCacheKey(storageKey));
        }
    }

    /**
     * 根据可见性返回对应的 R2 Cache-Control 值。
     */
    public static String cacheControlForVisibility(String visibility) {
        return "PUBLIC".equals(visibility) ? PUBLIC_CACHE_CONTROL : PRIVATE_CACHE_CONTROL;
    }

    private String createPrivateImageUrl(String storageKey) {
        String normalizedKey = normalizeKey(storageKey);
        long expiresAt = Instant.now().plus(PRIVATE_ACCESS_EXPIRY).getEpochSecond();
        String token = UUID.randomUUID().toString().replace("-", "");
        cacheService.put(tokenCacheKey(token), normalizedKey + "\n" + expiresAt, PRIVATE_ACCESS_EXPIRY);

        String url = mediaUrlUtil.getPublicUrl() + "/" + normalizedKey
                + "?" + TOKEN_QUERY_PARAM + "=" + token
                + "&expires=" + expiresAt;
        return withVersion(url, normalizedKey + expiresAt + ":PRIVATE");
    }

    private boolean privateUrlStillUsable(String url) {
        try {
            URI uri = URI.create(url);
            String query = uri.getRawQuery();
            if (query == null) {
                return false;
            }
            for (String part : query.split("&")) {
                int equals = part.indexOf('=');
                if (equals > 0 && "expires".equals(part.substring(0, equals))) {
                    long expiresAt = Long.parseLong(part.substring(equals + 1));
                    return Instant.now().plusSeconds(2).getEpochSecond() < expiresAt;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeKey(String storageKey) {
        return storageKey == null ? "" : storageKey.replaceAll("^/+", "");
    }

    private String urlCacheKey(String storageKey) {
        return URL_KEY_PREFIX + normalizeKey(storageKey);
    }

    private String tokenCacheKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }
}
