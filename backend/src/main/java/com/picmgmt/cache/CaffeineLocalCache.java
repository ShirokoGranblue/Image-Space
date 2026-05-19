package com.picmgmt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class CaffeineLocalCache {

    private final Cache<String, CacheEntry> cache;

    private record CacheEntry(Object value, long expireNanos) {}

    public CaffeineLocalCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfter(new Expiry<String, CacheEntry>() {
                    @Override
                    public long expireAfterCreate(String key, CacheEntry entry, long currentTime) {
                        return entry.expireNanos - currentTime;
                    }
                    @Override
                    public long expireAfterUpdate(String key, CacheEntry entry, long currentTime, long currentDuration) {
                        return entry.expireNanos - currentTime;
                    }
                    @Override
                    public long expireAfterRead(String key, CacheEntry entry, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .recordStats()
                .build();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry != null && type.isInstance(entry.value)) {
            return Optional.of((T) entry.value);
        }
        return Optional.empty();
    }

    public void put(String key, Object value, Duration ttl) {
        long expireNanos = System.nanoTime() + ttl.toNanos();
        cache.put(key, new CacheEntry(value, expireNanos));
    }

    public void evict(String key) {
        cache.invalidate(key);
    }

    public void evictByPattern(String pattern) {
        String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
        cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }
}
