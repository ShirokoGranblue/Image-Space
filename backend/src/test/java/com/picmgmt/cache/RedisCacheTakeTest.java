package com.picmgmt.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheTakeTest {

    @Test
    void takeUsesAtomicRedisGetAndDeleteAndEvictsLocalCopy() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        CaffeineLocalCache localCache = mock(CaffeineLocalCache.class);
        BloomFilterService bloomFilter = mock(BloomFilterService.class);
        CacheData<Long> cached = CacheData.of(7L, Long.MAX_VALUE);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("ticket")).thenReturn(cached);
        RedisCacheService service = new RedisCacheService(redisTemplate, localCache, bloomFilter);

        Optional<Long> result = service.take("ticket", Long.class);

        assertEquals(Optional.of(7L), result);
        verify(localCache).evict("ticket");
        verify(valueOperations).getAndDelete("ticket");
    }

    @Test
    void takeRejectsLogicallyExpiredValues() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("ticket"))
                .thenReturn(CacheData.of(7L, System.nanoTime() - 1));
        RedisCacheService service = new RedisCacheService(
                redisTemplate,
                mock(CaffeineLocalCache.class),
                mock(BloomFilterService.class)
        );

        assertEquals(Optional.empty(), service.take("ticket", Long.class));
    }
}
