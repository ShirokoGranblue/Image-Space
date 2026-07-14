package com.picmgmt.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.service.CaptchaService;
import com.picmgmt.service.OAuthService;
import com.picmgmt.service.TurnstileService;
import com.picmgmt.service.UserService;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.User;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
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

    @Test
    void avatarPrefersObjectStorageWhenAvailable() {
        User user = mediaUser("avatars/object.png", pngData("legacy"), null, null);
        byte[] stored = "stored".getBytes(StandardCharsets.UTF_8);
        when(userService.getByUuid("user-uuid")).thenReturn(user);
        when(storageService.download("avatars", "avatars/object.png")).thenReturn(stored);

        ResponseEntity<byte[]> response = controller.avatar("user-uuid");

        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
        assertArrayEquals(stored, response.getBody());
        verify(storageService, never()).upload(anyString(), anyString(), any(), anyString());
        verify(userService, never()).updateAvatar(any(), anyString());
    }

    @Test
    void avatarFallsBackToLegacyBase64WhenObjectIsMissing() {
        User user = mediaUser("avatars/missing.png", pngData("legacy-avatar"), null, null);
        when(userService.getByUuid("user-uuid")).thenReturn(user);
        when(storageService.download("avatars", "avatars/missing.png"))
                .thenThrow(new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED));

        ResponseEntity<byte[]> response = controller.avatar("user-uuid");

        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
        assertArrayEquals("legacy-avatar".getBytes(StandardCharsets.UTF_8), response.getBody());
        verify(storageService, never()).upload(anyString(), anyString(), any(), anyString());
        verify(userService, never()).updateAvatar(any(), anyString());
    }

    @Test
    void backgroundUsesLegacyMimeTypeWithoutWritingData() {
        User user = mediaUser(null, null, "backgrounds/missing.webp", webpData("legacy-background"));
        when(userService.getByUuid("user-uuid")).thenReturn(user);
        when(storageService.download("backgrounds", "backgrounds/missing.webp"))
                .thenThrow(new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED));

        ResponseEntity<byte[]> response = controller.background("user-uuid");

        assertEquals(MediaType.parseMediaType("image/webp"), response.getHeaders().getContentType());
        assertArrayEquals("legacy-background".getBytes(StandardCharsets.UTF_8), response.getBody());
        verify(storageService, never()).upload(anyString(), anyString(), any(), anyString());
        verify(userService, never()).updateBackground(any(), anyString());
    }

    @Test
    void invalidLegacyAvatarReturnsControlledNotFound() {
        User user = mediaUser(null, "data:image/png;base64,%%%", null, null);
        when(userService.getByUuid("user-uuid")).thenReturn(user);

        BusinessException error = assertThrows(BusinessException.class,
                () -> controller.avatar("user-uuid"));

        assertEquals(ErrorCode.NOT_FOUND, error.getErrorCode());
        verify(storageService, never()).download(anyString(), anyString());
        verify(storageService, never()).upload(anyString(), anyString(), any(), anyString());
    }

    private User mediaUser(String avatarKey, String avatar, String backgroundKey, String background) {
        User user = new User();
        user.setId(7L);
        user.setUuid("user-uuid");
        user.setAvatarKey(avatarKey);
        user.setAvatar(avatar);
        user.setBackgroundKey(backgroundKey);
        user.setBackground(background);
        return user;
    }

    private String pngData(String value) {
        return dataUri("image/png", value);
    }

    private String webpData(String value) {
        return dataUri("image/webp", value);
    }

    private String dataUri(String contentType, String value) {
        return "data:" + contentType + ";base64,"
                + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
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
