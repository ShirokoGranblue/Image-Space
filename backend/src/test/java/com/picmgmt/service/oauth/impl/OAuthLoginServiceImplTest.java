package com.picmgmt.service.oauth.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.auth.UserRole;
import com.picmgmt.auth.UserRoleMapper;
import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.common.BusinessException;
import com.picmgmt.dto.oauth.MicrosoftUserInfo;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.mapper.UserOauthAccountMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserOauthAccountMapper oauthAccountMapper;
    @Mock private BloomFilterService bloomFilterService;
    @Mock private UserRoleMapper userRoleMapper;
    @InjectMocks private OAuthLoginServiceImpl service;

    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        stpMock = mockStatic(StpUtil.class);
        stpMock.when(StpUtil::getTokenName).thenReturn("satoken");
        stpMock.when(StpUtil::getTokenValue).thenReturn("token");
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void microsoftDoesNotAutoLinkUnverifiedLocalEmail() {
        User existing = user(7L, 0);
        when(oauthAccountMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(existing);

        assertThrows(BusinessException.class,
                () -> service.loginOrRegisterByMicrosoft(userInfo()));

        verify(oauthAccountMapper, never()).insert(any());
        stpMock.verify(() -> StpUtil.login(any()), never());
    }

    @Test
    void microsoftCanLinkVerifiedLocalEmail() {
        User existing = user(7L, 1);
        when(oauthAccountMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(existing);
        when(userMapper.selectById(7L)).thenReturn(existing);

        var result = service.loginOrRegisterByMicrosoft(userInfo());

        assertEquals(7L, result.getUserId());
        verify(oauthAccountMapper).insert(any());
        stpMock.verify(() -> StpUtil.login(7L));
    }

    @Test
    void microsoftNewUserReceivesDefaultRole() {
        when(oauthAccountMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(9L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(userMapper.selectById(9L)).thenAnswer(invocation -> user(9L, 1));

        var result = service.loginOrRegisterByMicrosoft(userInfo());

        assertEquals(9L, result.getUserId());
        verify(userRoleMapper).insert(org.mockito.ArgumentMatchers.argThat(role ->
                role.getUserId().equals(9L) && role.getRoleId().equals(3L)
        ));
    }

    private User user(Long id, int emailVerified) {
        User user = new User();
        user.setId(id);
        user.setEmail("victim@example.com");
        user.setEmailVerified(emailVerified);
        user.setDeleted(0);
        return user;
    }

    private MicrosoftUserInfo userInfo() {
        MicrosoftUserInfo info = new MicrosoftUserInfo();
        info.setId("microsoft-subject");
        info.setUserPrincipalName("victim@example.com");
        info.setDisplayName("Victim");
        return info;
    }
}
