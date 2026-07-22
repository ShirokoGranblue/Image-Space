package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.auth.UserRoleMapper;
import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.common.BusinessException;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.mapper.UserOauthAccountMapper;
import com.picmgmt.storage.StorageService;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.model.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private StorageService storageService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private AuthStateCache authStateCache;
    @Mock private BloomFilterService bloomFilterService;
    @Mock private UserOauthAccountMapper oauthAccountMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @InjectMocks private OAuthServiceImpl service;

    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        stpMock = mockStatic(StpUtil.class);
        stpMock.when(StpUtil::getTokenValue).thenReturn("token");
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void oauthDoesNotAutoLinkUnverifiedLocalEmail() {
        User existing = new User();
        existing.setId(7L);
        existing.setEmailVerified(0);
        existing.setDeleted(0);
        when(oauthAccountMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(existing);

        assertThrows(BusinessException.class,
                () -> invokeLoginOrRegister("google", authUser(), "https://image-space.app"));

        stpMock.verify(() -> StpUtil.login(any()), never());
    }

    @Test
    void oauthNewUserReceivesDefaultRoleAndProviderBinding() throws Exception {
        when(oauthAccountMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(9L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        var result = invokeLoginOrRegister("google", authUser(), "https://image-space.app");

        assertEquals("token", result.token());
        verify(userMapper).insert(org.mockito.ArgumentMatchers.argThat(user ->
                "A".equals(user.getUsername()) && "A".equals(user.getDisplayName())
        ));
        verify(userRoleMapper).insert(org.mockito.ArgumentMatchers.argThat(role ->
                role.getUserId().equals(9L) && role.getRoleId().equals(3L)
        ));
        verify(oauthAccountMapper).insert(org.mockito.ArgumentMatchers.argThat(binding ->
                binding.getUserId().equals(9L)
                        && "google".equals(binding.getProvider())
                        && "provider-subject".equals(binding.getProviderUserId())
        ));
    }

    private com.picmgmt.service.OAuthService.OAuthResult invokeLoginOrRegister(
            String provider,
            AuthUser user,
            String redirectDomain
    ) throws Exception {
        Method method = OAuthServiceImpl.class.getDeclaredMethod(
                "loginOrRegister",
                String.class,
                AuthUser.class,
                String.class
        );
        method.setAccessible(true);
        try {
            return (com.picmgmt.service.OAuthService.OAuthResult)
                    method.invoke(service, provider, user, redirectDomain);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    private AuthUser authUser() {
        return AuthUser.builder()
                .uuid("provider-subject")
                .username("123@gmail.com")
                .nickname("A")
                .email("123@gmail.com")
                .source("GOOGLE")
                .build();
    }
}
