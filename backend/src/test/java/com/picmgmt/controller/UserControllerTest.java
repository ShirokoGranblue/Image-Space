package com.picmgmt.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.service.CaptchaService;
import com.picmgmt.service.OAuthService;
import com.picmgmt.service.TurnstileService;
import com.picmgmt.service.UserService;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.UserVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private StorageService storageService;
    @Mock private CaptchaService captchaService;
    @Mock private OAuthService oAuthService;
    @Mock private TurnstileService turnstileService;
    @Mock private StringRedisTemplate redisTemplate;

    private UserController controller;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        controller = new UserController(
                userService,
                storageService,
                captchaService,
                oAuthService,
                turnstileService,
                redisTemplate
        );
        stpMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void publicProfileDoesNotExposeEmailOrPhone() {
        stpMock.when(StpUtil::isLogin).thenReturn(false);
        when(userService.getUserVOByUuid("user-uuid")).thenReturn(user(7L));

        UserVO result = controller.profile("user-uuid").getData();

        assertNull(result.getEmail());
        assertNull(result.getPhone());
    }

    @Test
    void ownerProfileKeepsEmailAndPhone() {
        stpMock.when(StpUtil::isLogin).thenReturn(true);
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
        when(userService.getUserVOByUuid("user-uuid")).thenReturn(user(7L));

        UserVO result = controller.profile("user-uuid").getData();

        assertEquals("owner@example.com", result.getEmail());
        assertEquals("13800000000", result.getPhone());
    }

    private UserVO user(Long id) {
        UserVO user = new UserVO();
        user.setId(id);
        user.setUuid("user-uuid");
        user.setEmail("owner@example.com");
        user.setPhone("13800000000");
        return user;
    }
}
