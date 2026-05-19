package com.picmgmt.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CaffeineLocalCache caffeineLocalCache;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        // L1: Caffeine
        Optional<T> l1Result = caffeineLocalCache.get(key, type);
        if (l1Result.isPresent()) return l1Result;

        // L2: Redis
        Object value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            if (type.isInstance(value)) {
                caffeineLocalCache.put(key, value, Duration.ofMinutes(5));
                return Optional.of((T) value);
            }
            try {
                T converted = objectMapper.convertValue(value, type);
                caffeineLocalCache.put(key, converted, Duration.ofMinutes(5));
                return Optional.of(converted);
            } catch (Exception e) {
                log.debug("Redis value type conversion failed for key: {}", key);
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
        caffeineLocalCache.put(key, value, ttl);
    }

    @Override
    public void evict(String key) {
        redisTemplate.delete(key);
        caffeineLocalCache.evict(key);
        redisTemplate.convertAndSend("cache:invalidate", key);
    }

    @Override
    public void evictByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        caffeineLocalCache.evictByPattern(pattern);
        redisTemplate.convertAndSend("cache:invalidate:pattern", pattern);
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration ttl) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) return cached.get();
        T value = loader.get();
        if (value != null) {
            put(key, value, ttl);
        }
        return value;
    }

    @Override
    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        if (Boolean.TRUE.equals(result)) {
            caffeineLocalCache.put(key, value, ttl);
            return true;
        }
        return false;
    }
}
