package com.picmgmt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class CaffeineLocalCache {

    private final Cache<String, Object> cache;

    public CaffeineLocalCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = cache.getIfPresent(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    public void put(String key, Object value, Duration ttl) {
        cache.put(key, value);
    }

    public void evict(String key) {
        cache.invalidate(key);
    }

    public void evictByPattern(String pattern) {
        String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
        cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }
}
