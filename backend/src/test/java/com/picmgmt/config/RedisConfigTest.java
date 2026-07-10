package com.picmgmt.config;

import com.picmgmt.cache.CacheData;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    void cacheDataRetainsItsTypeAfterRedisSerializationRoundTrip() {
        RedisTemplate<String, Object> template =
                new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
        @SuppressWarnings("unchecked")
        RedisSerializer<Object> serializer =
                (RedisSerializer<Object>) template.getValueSerializer();
        CacheData<Long> original = CacheData.of(7L, Long.MAX_VALUE);

        Object restored = serializer.deserialize(serializer.serialize(original));

        CacheData<?> restoredCache = assertInstanceOf(CacheData.class, restored);
        assertEquals(7L, restoredCache.getData());

        Object restoredNull = serializer.deserialize(
                serializer.serialize(CacheData.nullMarker(Long.MAX_VALUE))
        );
        assertTrue(assertInstanceOf(CacheData.class, restoredNull).isNull());
    }
}
