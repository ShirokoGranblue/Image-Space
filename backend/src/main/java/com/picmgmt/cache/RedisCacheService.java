package com.picmgmt.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CaffeineLocalCache caffeineLocalCache;
    private final BloomFilterService bloomFilterService;

    private static final Duration PHYSICAL_TTL_EXTRA = Duration.ofMinutes(10);
    private static final Duration NULL_TTL = Duration.ofMinutes(1);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration L1_BACKFILL_TTL = Duration.ofMinutes(5);
    private static final int LOCK_RETRY_MS_MIN = 30;
    private static final int LOCK_RETRY_MS_MAX = 80;
    private static final int LOCK_MAX_RETRIES = 15;

    // ── Read ──

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        if (!bloomFilterService.mightContain(key)) {
            return Optional.empty();
        }

        // L1: Caffeine
        Optional<Object> l1 = caffeineLocalCache.getRaw(key);
        if (l1.isPresent()) {
            if (!isExpired(l1.get())) {
                return unwrapCacheData(l1.get(), type);
            }
            caffeineLocalCache.evict(key);
        }

        // L2: Redis
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            if (isExpired(value)) {
                evict(key);
                return Optional.empty();
            }
            caffeineLocalCache.put(key, value, L1_BACKFILL_TTL);
            return unwrapCacheData(value, type);
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> take(String key, Class<T> type) {
        caffeineLocalCache.evict(key);
        Object value = redisTemplate.opsForValue().getAndDelete(key);
        return isExpired(value) ? Optional.empty() : unwrapCacheData(value, type);
    }

    private boolean isExpired(Object value) {
        return value instanceof CacheData<?> cacheData && cacheData.isLogicallyExpired();
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> unwrapCacheData(Object raw, Class<T> type) {
        if (raw == null) {
            return Optional.empty();
        }
        if (!(raw instanceof CacheData<?> cd)) {
            if (type.isInstance(raw)) {
                return Optional.of((T) raw);
            }
            return Optional.empty();
        }
        if (cd.isNull()) {
            return Optional.empty();
        }
        Object data = cd.getData();
        if (data != null && type.isInstance(data)) {
            return Optional.of((T) data);
        }
        return Optional.empty();
    }

    // ── Write ──

    @Override
    public <T> void put(String key, T value, Duration logicalTtl) {
        Duration randomizedLogical = randomize(logicalTtl);
        Duration physicalTtl = logicalTtl.plus(PHYSICAL_TTL_EXTRA);
        long logicExpireNanos = System.nanoTime() + randomizedLogical.toNanos();

        CacheData<T> cd = CacheData.of(value, logicExpireNanos);
        redisTemplate.opsForValue().set(key, cd, physicalTtl);
        caffeineLocalCache.put(key, cd, randomizedLogical);
    }

    @Override
    public <T> void putExact(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
        caffeineLocalCache.put(key, value, ttl);
    }

    private void putNull(String key) {
        long logicExpireNanos = System.nanoTime() + NULL_TTL.toNanos();
        CacheData<?> cd = CacheData.nullMarker(logicExpireNanos);
        redisTemplate.opsForValue().set(key, cd, NULL_TTL.plus(PHYSICAL_TTL_EXTRA));
        caffeineLocalCache.put(key, cd, NULL_TTL);
    }

    private boolean isNullCached(String key) {
        Optional<Object> raw = caffeineLocalCache.getRaw(key);
        if (raw.isPresent() && raw.get() instanceof CacheData<?> cd && cd.isNull()) {
            if (!cd.isLogicallyExpired()) {
                return true;
            }
            caffeineLocalCache.evict(key);
        }
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof CacheData<?> cd && cd.isNull()) {
            if (!cd.isLogicallyExpired()) {
                return true;
            }
            evict(key);
        }
        return false;
    }

    private boolean isLogicallyExpired(String key) {
        Optional<Object> raw = caffeineLocalCache.getRaw(key);
        if (raw.isPresent() && raw.get() instanceof CacheData<?> cd) {
            return !cd.isNull() && cd.isLogicallyExpired();
        }
        return false;
    }

    // ── Evict ──

    @Override
    public void evict(String key) {
        redisTemplate.delete(key);
        caffeineLocalCache.evict(key);
    }

    @Override
    public void evictByPattern(String pattern) {
        try (var cursor = redisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
            List<String> keys = new java.util.ArrayList<>();
            while (cursor.hasNext()) {
                keys.add(cursor.next());
                if (keys.size() >= 100) {
                    redisTemplate.delete(keys);
                    keys.clear();
                }
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
        caffeineLocalCache.evictByPattern(pattern);
    }

    // ── getOrLoad with mutex + logical expiration async refresh ──

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration logicalTtl) {
        // Check L1 for logically expired data → return stale + async refresh
        Optional<Object> l1Raw = caffeineLocalCache.getRaw(key);
        if (l1Raw.isPresent() && l1Raw.get() instanceof CacheData<?> cd && !cd.isNull()) {
            if (cd.isLogicallyExpired()) {
                log.debug("Cache logically expired, returning stale + async refresh: {}", key);
                asyncRefresh(key, loader, logicalTtl);
            }
            if (type.isInstance(cd.getData())) {
                return (T) cd.getData();
            }
        }

        // Normal get path (bloom → L1 → L2)
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }

        if (isNullCached(key)) {
            return null;
        }

        return loadWithLock(key, type, loader, logicalTtl);
    }

    private void asyncRefresh(String key, Supplier<?> loader, Duration logicalTtl) {
        CompletableFuture.runAsync(() -> {
            try {
                Object fresh = loader.get();
                if (fresh != null) {
                    put(key, fresh, logicalTtl);
                }
            } catch (Exception e) {
                log.debug("Async refresh failed for key: {}", key, e.getMessage());
            }
        });
    }

    private <T> T loadWithLock(String key, Class<T> type, Supplier<T> loader, Duration logicalTtl) {
        String lockKey = "mutex:" + key;

        for (int i = 0; i < LOCK_MAX_RETRIES; i++) {
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_TTL);
            if (Boolean.TRUE.equals(locked)) {
                try {
                    // Double-check cache
                    Optional<T> cached = get(key, type);
                    if (cached.isPresent()) return cached.get();
                    if (isNullCached(key)) return null;

                    T value = loader.get();
                    if (value != null) {
                        put(key, value, logicalTtl);
                    } else {
                        putNull(key);
                    }
                    return value;
                } finally {
                    redisTemplate.delete(lockKey);
                }
            }

            try {
                Thread.sleep(LOCK_RETRY_MS_MIN +
                        ThreadLocalRandom.current().nextInt(LOCK_RETRY_MS_MAX - LOCK_RETRY_MS_MIN));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Last resort: direct load + cache result
        log.warn("Failed to acquire mutex lock after {} retries, loading directly: {}",
                LOCK_MAX_RETRIES, key);
        T value = loader.get();
        if (value != null) {
            put(key, value, logicalTtl);
        } else {
            putNull(key);
        }
        return value;
    }

    // ── setIfAbsent ──

    @Override
    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        if (Boolean.TRUE.equals(result)) {
            caffeineLocalCache.put(key, value, ttl);
            return true;
        }
        return false;
    }

    // ── TTL randomization ──

    private Duration randomize(Duration ttl) {
        long millis = ttl.toMillis();
        if (millis < 2000) return ttl;
        long jitter = (long)(millis * 0.2 * ThreadLocalRandom.current().nextDouble());
        boolean plus = ThreadLocalRandom.current().nextBoolean();
        return Duration.ofMillis(plus ? millis + jitter : millis - jitter);
    }
}
