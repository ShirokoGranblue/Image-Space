package com.picmgmt.image;

import com.picmgmt.cache.CacheService;
import com.picmgmt.util.MediaUrlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUrlService {

    private final CacheService cacheService;
    private final MediaUrlUtil mediaUrlUtil;

    public static final Duration PRIVATE_ACCESS_EXPIRY = Duration.ofSeconds(30);

    public static final String PUBLIC_CACHE_CONTROL = "public, max-age=31536000, immutable";
    public static final String PRIVATE_CACHE_CONTROL = "no-store";

    private static final String URL_KEY_PREFIX = "media:url:";
    private static final String TOKEN_KEY_PREFIX = "media:token:";
    private static final String TOKEN_INDEX_KEY_PREFIX = "media:token:index:";
    private static final String TOKEN_QUERY_PARAM = "auth";
    private static final Duration PRIVATE_URL_CACHE_TTL = PRIVATE_ACCESS_EXPIRY.minus(Duration.ofSeconds(5));

    public String getPublicImageUrl(String storageKey, Long mediaVersion) {
        String normalizedKey = normalizeKey(storageKey);
        if (normalizedKey.isBlank()) {
            return null;
        }
        long version = normalizeVersion(mediaVersion);
        return mediaUrlUtil.getPublicUrl() + "/public/" + normalizedKey + "?v=" + version;
    }

    public String getPrivateImageUrl(String storageKey) {
        String normalizedKey = normalizeKey(storageKey);
        if (normalizedKey.isBlank()) {
            return null;
        }

        String cacheKey = urlCacheKey(normalizedKey);
        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent() && privateUrlStillUsable(cached.get())) {
            return cached.get();
        }
        cached.ifPresent(ignored -> cacheService.evict(cacheKey));

        String url = createPrivateImageUrl(normalizedKey);
        cacheService.put(cacheKey, url, PRIVATE_URL_CACHE_TTL);
        return url;
    }

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

    public void evictPrivateAccess(String storageKey) {
        String normalizedKey = normalizeKey(storageKey);
        if (normalizedKey.isBlank()) {
            return;
        }

        cacheService.evict(urlCacheKey(normalizedKey));
        String indexKey = tokenIndexKey(normalizedKey);
        Optional<String> indexedTokens = cacheService.get(indexKey, String.class);
        indexedTokens.ifPresent(tokens -> {
            for (String token : tokens.split("\\n")) {
                if (!token.isBlank()) {
                    cacheService.evict(tokenCacheKey(token.trim()));
                }
            }
        });
        cacheService.evict(indexKey);
    }

    public static String cacheControlForVisibility(String visibility) {
        return "PUBLIC".equalsIgnoreCase(visibility) ? PUBLIC_CACHE_CONTROL : PRIVATE_CACHE_CONTROL;
    }

    private String createPrivateImageUrl(String normalizedKey) {
        long expiresAt = Instant.now().plus(PRIVATE_ACCESS_EXPIRY).getEpochSecond();
        String token = UUID.randomUUID().toString().replace("-", "");
        cacheService.put(tokenCacheKey(token), normalizedKey + "\n" + expiresAt, PRIVATE_ACCESS_EXPIRY);
        indexToken(normalizedKey, token);

        return mediaUrlUtil.getPublicUrl() + "/private/" + normalizedKey
                + "?" + TOKEN_QUERY_PARAM + "=" + token
                + "&expires=" + expiresAt;
    }

    private void indexToken(String normalizedKey, String token) {
        String indexKey = tokenIndexKey(normalizedKey);
        Set<String> tokens = new LinkedHashSet<>();
        cacheService.get(indexKey, String.class)
                .ifPresent(existing -> {
                    for (String value : existing.split("\\n")) {
                        if (!value.isBlank()) {
                            tokens.add(value.trim());
                        }
                    }
                });
        tokens.add(token);
        cacheService.put(indexKey, String.join("\n", tokens), PRIVATE_ACCESS_EXPIRY);
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

    private long normalizeVersion(Long mediaVersion) {
        return mediaVersion == null || mediaVersion < 1 ? 1 : mediaVersion;
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

    private String tokenIndexKey(String storageKey) {
        return TOKEN_INDEX_KEY_PREFIX + normalizeKey(storageKey);
    }
}
