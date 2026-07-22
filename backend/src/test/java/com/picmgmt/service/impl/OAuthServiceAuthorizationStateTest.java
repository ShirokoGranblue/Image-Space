package com.picmgmt.service.impl;

import com.picmgmt.auth.UserRoleMapper;
import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.mapper.UserOauthAccountMapper;
import com.picmgmt.service.oauth.RedisAuthStateCache;
import com.picmgmt.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceAuthorizationStateTest {

    @Mock private UserMapper userMapper;
    @Mock private StorageService storageService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private BloomFilterService bloomFilterService;
    @Mock private UserOauthAccountMapper oauthAccountMapper;
    @Mock private UserRoleMapper userRoleMapper;

    private final Map<String, String> storedValues = new HashMap<>();
    private RedisAuthStateCache stateCache;
    private OAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> {
            storedValues.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(redisTemplate.hasKey(anyString()))
                .thenAnswer(invocation -> storedValues.containsKey(invocation.getArgument(0)));

        stateCache = new RedisAuthStateCache(redisTemplate);
        service = new OAuthServiceImpl(
                userMapper,
                storageService,
                redisTemplate,
                stateCache,
                bloomFilterService,
                oauthAccountMapper,
                userRoleMapper
        );
        ReflectionTestUtils.setField(service, "googleClientId", "test-client-id");
        ReflectionTestUtils.setField(service, "googleClientSecret", "test-client-secret");
        ReflectionTestUtils.setField(service, "googleRedirectUri", "https://image-space.app/user/oauth/google/callback");
        ReflectionTestUtils.setField(service, "githubClientId", "test-client-id");
        ReflectionTestUtils.setField(service, "githubClientSecret", "test-client-secret");
        ReflectionTestUtils.setField(service, "githubRedirectUri", "https://image-space.app/user/oauth/github/callback");
    }

    @Test
    void generatedGoogleStateIsAvailableFromSharedCache() {
        String authorizeUrl = service.getAuthorizeUrl("google", "https://image-space.app");

        assertTrue(stateCache.containsKey(stateFrom(authorizeUrl)));
    }

    @Test
    void generatedGithubStateIsAvailableFromSharedCache() {
        String authorizeUrl = service.getAuthorizeUrl("github", "https://image-space.app");

        assertTrue(stateCache.containsKey(stateFrom(authorizeUrl)));
    }

    private String stateFrom(String authorizeUrl) {
        return UriComponentsBuilder.fromUriString(authorizeUrl)
                .build()
                .getQueryParams()
                .getFirst("state");
    }
}
