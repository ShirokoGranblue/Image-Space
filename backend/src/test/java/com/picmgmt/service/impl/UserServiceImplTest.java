package com.picmgmt.service.impl;

import com.picmgmt.auth.UserRoleMapper;
import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.cache.RedisCacheService;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.repository.UserRepository;
import com.picmgmt.service.CaptchaService;
import com.picmgmt.service.EmailService;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserMapper userMapper;
    @Mock private UserRepository userRepository;
    @Mock private RedisCacheService redisCacheService;
    @Mock private EmailService emailService;
    @Mock private CaptchaService captchaService;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private BloomFilterService bloomFilterService;
    @Mock private StorageService storageService;

    @InjectMocks private UserServiceImpl service;

    @Test
    void updateProfileRejectsEmailChangeWithoutVerificationCode() {
        User user = user("old@example.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateProfile(7L, "name", "new@example.com", null, null)
        );

        assertEquals(ErrorCode.CODE_INVALID, exception.getErrorCode());
        verify(userRepository, never()).updateById(any());
    }

    @Test
    void verifiedEmailChangeUpdatesEmailAndConsumesCode() throws Exception {
        User user = user("old@example.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(redisCacheService.get(
                "code:change_email:7:new@example.com",
                String.class
        )).thenReturn(Optional.of("123456"));
        when(redisCacheService.get(
                "code:attempts:change_email:7:new@example.com",
                Integer.class
        )).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            User updated = invocation.getArgument(0);
            return updated;
        }).when(userRepository).updateById(any(User.class));
        when(userRepository.toVO(any(User.class))).thenAnswer(invocation -> {
            User updated = invocation.getArgument(0);
            UserVO vo = new UserVO();
            vo.setEmail(updated.getEmail());
            return vo;
        });

        UserVO result = invokeVerifiedUpdate("new@example.com", "123456");

        assertEquals("new@example.com", result.getEmail());
        assertEquals(1, user.getEmailVerified());
        verify(redisCacheService).evict("code:change_email:7:new@example.com");
    }

    @Test
    void sendEmailChangeCodeStoresCodeForUserAndTargetEmail() throws Exception {
        User user = user("old@example.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(redisCacheService.setIfAbsent(
                "code:cooldown:change_email:7:new@example.com",
                "1",
                Duration.ofSeconds(60)
        )).thenReturn(true);

        invokeSendEmailChangeCode("new@example.com");

        verify(emailService).sendVerificationCode(eq("new@example.com"), any(String.class));
        verify(redisCacheService).putExact(
                eq("code:change_email:7:new@example.com"),
                any(String.class),
                eq(Duration.ofSeconds(300))
        );
    }

    @Test
    void updateAvatarDeletesPreviousObjectAfterDatabaseUpdate() {
        User user = user("owner@example.com");
        user.setAvatarKey("avatars/old.png");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        service.updateAvatar(7L, "avatars/new.png");

        verify(userRepository).updateById(user);
        verify(storageService).deleteObjectIfExists("avatars", "avatars/old.png");
    }

    @Test
    void updateAvatarDeletesNewObjectWhenDatabaseUpdateFails() {
        User user = user("owner@example.com");
        user.setAvatarKey("avatars/old.png");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("db failed")).when(userRepository).updateById(user);

        assertThrows(RuntimeException.class,
                () -> service.updateAvatar(7L, "avatars/new.png"));

        verify(storageService).deleteObjectIfExists("avatars", "avatars/new.png");
        verify(storageService, never()).deleteObjectIfExists("avatars", "avatars/old.png");
    }

    @Test
    void updateBackgroundDeletesPreviousObjectAfterDatabaseUpdate() {
        User user = user("owner@example.com");
        user.setBackgroundKey("backgrounds/old.png");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        service.updateBackground(7L, "backgrounds/new.png");

        verify(userRepository).updateById(user);
        verify(storageService).deleteObjectIfExists("backgrounds", "backgrounds/old.png");
    }

    private UserVO invokeVerifiedUpdate(String email, String code) throws Exception {
        Method method = UserServiceImpl.class.getMethod(
                "updateProfile",
                Long.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        try {
            return (UserVO) method.invoke(service, 7L, "name", email, null, null, code);
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
    }

    private void invokeSendEmailChangeCode(String email) throws Exception {
        Method method = UserServiceImpl.class.getMethod("sendEmailChangeCode", Long.class, String.class);
        try {
            method.invoke(service, 7L, email);
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
    }

    private Exception unwrap(InvocationTargetException exception) {
        return exception.getCause() instanceof Exception cause ? cause : exception;
    }

    private User user(String email) {
        User user = new User();
        user.setId(7L);
        user.setEmail(email);
        user.setEmailVerified(1);
        return user;
    }
}
