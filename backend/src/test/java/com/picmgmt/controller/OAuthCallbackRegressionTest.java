package com.picmgmt.controller;

import com.picmgmt.auth.UserRoleMapper;
import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.mapper.UserOauthAccountMapper;
import com.picmgmt.service.CaptchaService;
import com.picmgmt.service.TurnstileService;
import com.picmgmt.service.UserService;
import com.picmgmt.service.impl.OAuthServiceImpl;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OAuthCallbackRegressionTest {

    @Mock private UserService userService;
    @Mock private StorageService storageService;
    @Mock private CaptchaService captchaService;
    @Mock private TurnstileService turnstileService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private BloomFilterService bloomFilterService;
    @Mock private UserMapper userMapper;
    @Mock private UserOauthAccountMapper oauthAccountMapper;
    @Mock private UserRoleMapper userRoleMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        OAuthServiceImpl oAuthService = new OAuthServiceImpl(
                userMapper,
                storageService,
                redisTemplate,
                new RedisAuthStateCache(redisTemplate),
                bloomFilterService,
                oauthAccountMapper,
                userRoleMapper
        );
        ReflectionTestUtils.setField(oAuthService, "googleClientId", "test-client-id");
        ReflectionTestUtils.setField(oAuthService, "googleClientSecret", "test-client-secret");
        ReflectionTestUtils.setField(oAuthService, "googleRedirectUri", "https://image-space.app/user/oauth/google/callback");
        ReflectionTestUtils.setField(oAuthService, "githubClientId", "test-client-id");
        ReflectionTestUtils.setField(oAuthService, "githubClientSecret", "test-client-secret");
        ReflectionTestUtils.setField(oAuthService, "githubRedirectUri", "https://image-space.app/user/oauth/github/callback");

        UserController controller = new UserController(
                userService,
                storageService,
                captchaService,
                oAuthService,
                turnstileService,
                redisTemplate
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void expiredGoogleStateRedirectsBackToLoginWithRecoverableError() throws Exception {
        mockMvc.perform(get("/user/oauth/google/callback")
                        .param("code", "unused-code")
                        .param("state", "expired-state")
                        .header("Host", "image-space.app")
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://image-space.app/login?oauthError=state_invalid"));
    }

    @Test
    void expiredGithubStateRedirectsBackToLoginWithRecoverableError() throws Exception {
        mockMvc.perform(get("/user/oauth/github/callback")
                        .param("code", "unused-code")
                        .param("state", "expired-state")
                        .header("Host", "image-space.app")
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://image-space.app/login?oauthError=state_invalid"));
    }
}
