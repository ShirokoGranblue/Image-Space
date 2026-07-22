package com.picmgmt.service.oauth;

import me.zhyd.oauth.cache.AuthStateCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisAuthStateCache implements AuthStateCache {

    private static final String KEY_PREFIX = "oauth:state:";
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public RedisAuthStateCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void cache(String key, String value) {
        redisTemplate.opsForValue().set(redisKey(key), value, DEFAULT_TTL);
    }

    @Override
    public void cache(String key, String value, long timeout) {
        redisTemplate.opsForValue().set(redisKey(key), value, Duration.ofMillis(timeout));
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(redisKey(key));
    }

    @Override
    public boolean containsKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey(key)));
    }

    private String redisKey(String key) {
        return KEY_PREFIX + key;
    }
}
