package com.picmgmt.service.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAuthStateCacheTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private final Map<String, String> storedValues = new HashMap<>();
    private final AtomicReference<Duration> storedTtl = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> {
            storedValues.put(invocation.getArgument(0), invocation.getArgument(1));
            storedTtl.set(invocation.getArgument(2));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(valueOperations.get(anyString()))
                .thenAnswer(invocation -> storedValues.get(invocation.getArgument(0)));
        when(redisTemplate.hasKey(anyString()))
                .thenAnswer(invocation -> storedValues.containsKey(invocation.getArgument(0)));
    }

    @Test
    void defaultCacheKeepsOAuthStateForTenMinutes() {
        RedisAuthStateCache cache = new RedisAuthStateCache(redisTemplate);

        cache.cache("state-123", "state-123");

        assertTrue(cache.containsKey("state-123"));
        assertEquals("state-123", cache.get("state-123"));
        assertEquals(Duration.ofMinutes(10), storedTtl.get());
    }
}
