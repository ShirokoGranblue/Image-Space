package com.picmgmt.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.annotation.Audit;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.common.Result;
import com.picmgmt.entity.AuditLog;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.AuditLogMapper;
import com.picmgmt.mapper.UserMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock private AuditLogMapper auditLogMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature signature;

    private AuditAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new AuditAspect(auditLogMapper, new ObjectMapper(), userMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/image/image-uuid");
        request.addHeader("CF-Connecting-IP", "203.0.113.8");
        request.addHeader("User-Agent", "JUnit Browser");
        request.addHeader("Authorization", "Bearer secret-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void around_shouldInsertSuccessAuditLogWithRequestMetadataAndTarget() throws Throwable {
        User user = new User();
        user.setUsername("alice");
        when(userMapper.selectById(7L)).thenReturn(user);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "safe text");
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.png", "image/png", "image bytes".getBytes(StandardCharsets.UTF_8));
        Audit audit = stubJoinPoint("auditedImageUpdate", "image-uuid", body, file);
        Result<Map<String, String>> response = Result.ok(Map.of("uuid", "response-uuid"));
        when(joinPoint.proceed()).thenReturn(response);

        Object result;
        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::isLogin).thenReturn(true);
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            result = aspect.around(joinPoint, audit);
        }

        assertSame(response, result);
        AuditLog log = capturedLog();
        assertEquals(7L, log.getUserId());
        assertEquals("alice", log.getUsername());
        assertEquals("IMAGE_UPDATE", log.getAction());
        assertEquals("IMAGE", log.getModule());
        assertEquals("image", log.getTargetType());
        assertEquals("image-uuid", log.getTargetId());
        assertEquals("PUT", log.getMethod());
        assertEquals("/image/image-uuid", log.getPath());
        assertEquals("203.0.113.8", log.getIp());
        assertEquals("JUnit Browser", log.getUserAgent());
        assertEquals("SUCCESS", log.getResult());
        assertTrue(log.getRequestParams().contains("\"uuid\":\"image-uuid\""));
        assertTrue(log.getRequestParams().contains("\"originalFilename\":\"sample.png\""));
        assertFalse(log.getRequestParams().contains("image bytes"));
    }

    @Test
    void around_shouldInsertFailAuditLogAndRethrowOriginalException() throws Throwable {
        User user = new User();
        user.setUsername("alice");
        when(userMapper.selectById(7L)).thenReturn(user);
        Audit audit = stubJoinPoint("auditedCommentDelete", 42L);
        BusinessException expected = new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        when(joinPoint.proceed()).thenThrow(expected);

        BusinessException thrown;
        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::isLogin).thenReturn(true);
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            thrown = assertThrows(BusinessException.class, () -> aspect.around(joinPoint, audit));
        }

        assertSame(expected, thrown);
        AuditLog log = capturedLog();
        assertEquals("COMMENT_DELETE", log.getAction());
        assertEquals("comment", log.getTargetType());
        assertEquals("42", log.getTargetId());
        assertEquals("FAIL", log.getResult());
        assertTrue(log.getErrorMessage().contains(expected.getMessage()));
    }

    @Test
    void around_shouldSanitizeSensitiveValuesAndTruncateLongRequestParams() throws Throwable {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("password", "plain-password");
        body.put("turnstileToken", "turnstile-secret");
        body.put("captchaCode", "123456");
        body.put("code", "654321");
        body.put("Authorization", "Bearer nested-secret");
        body.put("description", "x".repeat(5000));
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.png", "image/png", "file-content".getBytes(StandardCharsets.UTF_8));
        Audit audit = stubJoinPoint("auditedImageUpdate", "image-uuid", body, file);
        when(joinPoint.proceed()).thenReturn(Result.ok());

        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::isLogin).thenReturn(false);
            aspect.around(joinPoint, audit);
        }

        String params = capturedLog().getRequestParams();
        assertTrue(params.length() <= 4000);
        assertTrue(params.contains("[FILTERED]"));
        assertFalse(params.contains("plain-password"));
        assertFalse(params.contains("turnstile-secret"));
        assertFalse(params.contains("123456"));
        assertFalse(params.contains("654321"));
        assertFalse(params.contains("nested-secret"));
        assertFalse(params.contains("file-content"));
    }

    private Audit stubJoinPoint(String methodName, Object... args) throws NoSuchMethodException {
        Method method = switch (methodName) {
            case "auditedImageUpdate" -> DummyController.class.getDeclaredMethod(
                    methodName, String.class, Map.class, MultipartFile.class);
            case "auditedCommentDelete" -> DummyController.class.getDeclaredMethod(methodName, Long.class);
            default -> throw new IllegalArgumentException(methodName);
        };
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(parameterNames(methodName));
        when(joinPoint.getArgs()).thenReturn(args);
        return method.getAnnotation(Audit.class);
    }

    private String[] parameterNames(String methodName) {
        return switch (methodName) {
            case "auditedImageUpdate" -> new String[] {"uuid", "body", "file"};
            case "auditedCommentDelete" -> new String[] {"id"};
            default -> new String[0];
        };
    }

    private AuditLog capturedLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        return captor.getValue();
    }

    private static class DummyController {
        @Audit(action = "IMAGE_UPDATE", module = "IMAGE", targetType = "image")
        void auditedImageUpdate(@PathVariable("uuid") String uuid,
                                @RequestBody Map<String, Object> body,
                                @RequestParam("file") MultipartFile file) {
        }

        @Audit(action = "COMMENT_DELETE", module = "COMMENT", targetType = "comment")
        void auditedCommentDelete(@PathVariable("id") Long id) {
        }
    }
}
