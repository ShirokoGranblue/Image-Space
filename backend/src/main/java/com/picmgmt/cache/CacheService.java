package com.picmgmt.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface CacheService {

    <T> Optional<T> get(String key, Class<T> type);

    <T> void put(String key, T value, Duration ttl);

    void evict(String key);

    void evictByPattern(String pattern);

    <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, Duration ttl);
}
