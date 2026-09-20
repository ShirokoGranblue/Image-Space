package com.picmgmt.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.annotation.Audit;
import com.picmgmt.audit.AuditLogEventPublisher;
import com.picmgmt.audit.AuditLogSanitizer;
import com.picmgmt.audit.AuditRiskEvaluator;
import com.picmgmt.common.Result;
import com.picmgmt.entity.AuditLog;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.AuditLogMapper;
import com.picmgmt.mapper.UserMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final int REQUEST_PARAMS_MAX_LENGTH = 4000;
    private static final int RESPONSE_RESULT_MAX_LENGTH = 4000;
    private static final int USERNAME_MAX_LENGTH = 100;
    private static final int ACTION_MAX_LENGTH = 100;
    private static final int MODULE_MAX_LENGTH = 100;
    private static final int TARGET_TYPE_MAX_LENGTH = 100;
    private static final int TARGET_ID_MAX_LENGTH = 100;
    private static final int METHOD_MAX_LENGTH = 10;
    private static final int PATH_MAX_LENGTH = 500;
    private static final int IP_MAX_LENGTH = 64;
    private static final int USER_AGENT_MAX_LENGTH = 500;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;
    private static final String FILTERED = AuditLogSanitizer.FILTERED;
    private static final List<String> TARGET_KEYS = List.of("uuid", "id", "imageId", "commentId");

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    @Autowired(required = false)
    private AuditLogSanitizer auditLogSanitizer;

    @Autowired(required = false)
    private AuditRiskEvaluator auditRiskEvaluator;

    @Autowired(required = false)
    private AuditLogEventPublisher auditLogEventPublisher;

    @Autowired(required = false)
    private com.picmgmt.messaging.OutboxService outboxService;

    private final java.util.concurrent.atomic.AtomicLong auditWriteFailures = new java.util.concurrent.atomic.AtomicLong();

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        HttpServletRequest request = currentRequest();
        Map<String, Object> requestParams = collectRequestParams(joinPoint);
        UserSnapshot beforeUser = currentUser();
        Object result = null;
        Throwable error = null;
        long startNanos = System.nanoTime();

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            long costTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            UserSnapshot afterUser = currentUser();
            AuditLog logEntry = buildLog(audit, request, requestParams, beforeUser, afterUser, result, error, costTime);
            insertQuietly(logEntry);
        }
    }

    private AuditLog buildLog(Audit audit, HttpServletRequest request, Map<String, Object> requestParams,
                              UserSnapshot beforeUser, UserSnapshot afterUser, Object result, Throwable error,
                              Long costTime) {
        UserSnapshot user = chooseUser(beforeUser, afterUser);
        String status = auditStatus(result, error);
        AuditLog logEntry = new AuditLog();
        logEntry.setUserId(user.userId());
        logEntry.setUsername(truncate(firstNonBlank(
                user.username(),
                findStringValue(requestParams, "username"),
                findStringValue(requestParams, "email"),
                findResultValue(result, "username"),
                findResultValue(result, "email")
        ), USERNAME_MAX_LENGTH));
        logEntry.setAction(truncate(audit.action(), ACTION_MAX_LENGTH));
        logEntry.setModule(truncate(audit.module(), MODULE_MAX_LENGTH));
        logEntry.setTargetType(truncate(audit.targetType(), TARGET_TYPE_MAX_LENGTH));
        logEntry.setTargetId(truncate(resolveTargetId(audit, requestParams, user, result), TARGET_ID_MAX_LENGTH));
        logEntry.setMethod(truncate(request == null ? null : request.getMethod(), METHOD_MAX_LENGTH));
        logEntry.setPath(truncate(request == null ? null : request.getRequestURI(), PATH_MAX_LENGTH));
        logEntry.setIp(truncate(clientIp(request), IP_MAX_LENGTH));
        logEntry.setUserAgent(truncate(request == null ? null : request.getHeader("User-Agent"), USER_AGENT_MAX_LENGTH));
        logEntry.setRequestParams(toJson(requestParams));
        logEntry.setResponseResult(auditResponseResult(result));
        logEntry.setResult(auditResult(result, error));
        logEntry.setStatus(status);
        logEntry.setRiskLevel(riskEvaluator().evaluate(audit.action(), audit.module(), status, logEntry.getPath()));
        logEntry.setCostTime(costTime == null ? 0L : Math.max(0L, costTime));
        logEntry.setErrorMessage(auditErrorMessage(result, error));
        logEntry.setCreateTime(LocalDateTime.now());
        return logEntry;
    }

    private String resolveTargetId(Audit audit, Map<String, Object> requestParams, UserSnapshot user, Object result) {
        String explicitResult = findConfiguredResultValue(result, audit.targetIdResult());
        String explicitParam = findConfiguredParamValue(requestParams, audit.targetIdParam());
        if (hasConfiguredTarget(audit)) {
            return firstNonBlank(explicitResult, explicitParam, userTargetId(audit, user));
        }
        return firstNonBlank(
                findTargetId(requestParams),
                findResultValue(result, "uuid"),
                findResultValue(result, "id"),
                userTargetId(audit, user)
        );
    }

    private boolean hasConfiguredTarget(Audit audit) {
        return (audit.targetIdResult() != null && !audit.targetIdResult().isBlank())
                || (audit.targetIdParam() != null && !audit.targetIdParam().isBlank());
    }

    private String findConfiguredResultValue(Object result, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return findResultValue(result, key);
    }

    private String findConfiguredParamValue(Map<String, Object> requestParams, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String value = findStringValue(requestParams, key);
        if (value == null || value.isBlank() || FILTERED.equals(value)) {
            return null;
        }
        return value;
    }

    private String userTargetId(Audit audit, UserSnapshot user) {
        if ("user".equalsIgnoreCase(audit.targetType()) && user.hasUser()) {
            return String.valueOf(user.userId());
        }
        return null;
    }

    private String auditStatus(Object result, Throwable error) {
        if (error != null) {
            return "FAILED";
        }
        if (result instanceof Result<?> apiResult && apiResult.getCode() != 200) {
            return "FAILED";
        }
        return "SUCCESS";
    }

    private String auditResult(Object result, Throwable error) {
        if (error != null) {
            return "FAIL";
        }
        if (result instanceof Result<?> apiResult && apiResult.getCode() != 200) {
            return "FAIL";
        }
        return "SUCCESS";
    }

    private String auditErrorMessage(Object result, Throwable error) {
        if (error != null) {
            return sanitizer().sanitizeText(error.getMessage(), ERROR_MESSAGE_MAX_LENGTH);
        }
        if (result instanceof Result<?> apiResult && apiResult.getCode() != 200) {
            return sanitizer().sanitizeText(apiResult.getMessage(), ERROR_MESSAGE_MAX_LENGTH);
        }
        return null;
    }

    private Map<String, Object> collectRequestParams(ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (!(joinPoint.getSignature() instanceof MethodSignature methodSignature)) {
            return params;
        }
        Method method = methodSignature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = methodSignature.getParameterNames();
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (shouldSkipArgument(arg)) {
                continue;
            }
            String name = resolveParameterName(i, parameterNames, annotations);
            params.put(name, sanitizeValue(name, arg));
        }
        return params;
    }

    private String resolveParameterName(int index, String[] parameterNames, Annotation[][] annotations) {
        if (annotations != null && index < annotations.length) {
            for (Annotation annotation : annotations[index]) {
                if (annotation instanceof PathVariable pathVariable) {
                    return annotationName(pathVariable.name(), pathVariable.value(), fallbackName(index, parameterNames));
                }
                if (annotation instanceof RequestParam requestParam) {
                    return annotationName(requestParam.name(), requestParam.value(), fallbackName(index, parameterNames));
                }
                if (annotation instanceof RequestBody) {
                    return fallbackName(index, parameterNames);
                }
            }
        }
        return fallbackName(index, parameterNames);
    }

    private String annotationName(String name, String value, String fallback) {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback;
    }

    private String fallbackName(int index, String[] parameterNames) {
        if (parameterNames != null && index < parameterNames.length
                && parameterNames[index] != null && !parameterNames[index].isBlank()) {
            return parameterNames[index];
        }
        return "arg" + index;
    }

    private boolean shouldSkipArgument(Object value) {
        return value instanceof ServletRequest
                || value instanceof ServletResponse
                || value instanceof BindingResult
                || value instanceof InputStream
                || value instanceof OutputStream;
    }

    private Object sanitizeValue(String key, Object value) {
        return sanitizer().sanitizeValue(key, value);
    }

    private boolean isSensitiveKey(String key) {
        return sanitizer().isSensitiveKey(key);
    }

    private String toJson(Map<String, Object> requestParams) {
        return sanitizer().toSanitizedJson(requestParams, REQUEST_PARAMS_MAX_LENGTH);
    }

    private String auditResponseResult(Object result) {
        if (result == null) {
            return null;
        }
        return sanitizer().toSanitizedJson(result, RESPONSE_RESULT_MAX_LENGTH);
    }

    private String findTargetId(Map<String, Object> params) {
        for (String key : TARGET_KEYS) {
            String value = findStringValue(params, key);
            if (value != null && !value.isBlank() && !FILTERED.equals(value)) {
                return value;
            }
        }
        return null;
    }

    private String findStringValue(Object value, String key) {
        Object found = findValue(value, key);
        return found == null ? null : String.valueOf(found);
    }

    private Object findValue(Object value, String key) {
        if (value == null || key == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (key.equals(String.valueOf(entry.getKey()))) {
                    return entry.getValue();
                }
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object nested = findValue(entry.getValue(), key);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                Object nested = findValue(item, key);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        if (value instanceof Result<?> result) {
            return findValue(result.getData(), key);
        }
        if (value.getClass().isArray() && !(value instanceof byte[])) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object nested = findValue(Array.get(value, i), key);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        return findPojoValue(value, key);
    }

    private Object findPojoValue(Object value, String key) {
        if (value instanceof CharSequence || value instanceof Number
                || value instanceof Boolean || value instanceof Enum<?>
                || value instanceof MultipartFile || value instanceof ServletRequest
                || value instanceof ServletResponse) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
            return findValue(map, key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String findResultValue(Object result, String key) {
        if (!(result instanceof Result<?> apiResult)) {
            return null;
        }
        Object data = apiResult.getData();
        Object found = findValue(data, key);
        return found == null ? null : String.valueOf(found);
    }

    private UserSnapshot chooseUser(UserSnapshot beforeUser, UserSnapshot afterUser) {
        if (afterUser.hasUser()) {
            return afterUser;
        }
        if (beforeUser.hasUser()) {
            return beforeUser;
        }
        return UserSnapshot.empty();
    }

    private UserSnapshot currentUser() {
        try {
            if (!StpUtil.isLogin()) {
                return UserSnapshot.empty();
            }
            Long userId = StpUtil.getLoginIdAsLong();
            String username = null;
            if (userId != null) {
                User user = userMapper.selectById(userId);
                if (user != null) {
                    username = user.getUsername();
                }
            }
            return new UserSnapshot(userId, username);
        } catch (Exception ignored) {
            return UserSnapshot.empty();
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private static final Set<String> TRUSTED_PROXY_PREFIXES = Set.of(
            "127.0.0.1", "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
            "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.",
            "192.168.", "::1"
    );

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String remoteAddr = request.getRemoteAddr();
        boolean fromTrustedProxy = TRUSTED_PROXY_PREFIXES.stream().anyMatch(remoteAddr::startsWith);
        if (fromTrustedProxy) {
            String cfIp = request.getHeader("CF-Connecting-IP");
            if (cfIp != null && !cfIp.isBlank()) {
                return cfIp.trim();
            }
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return remoteAddr;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private AuditLogSanitizer sanitizer() {
        if (auditLogSanitizer == null) {
            auditLogSanitizer = new AuditLogSanitizer(objectMapper);
        }
        return auditLogSanitizer;
    }

    private AuditRiskEvaluator riskEvaluator() {
        if (auditRiskEvaluator == null) {
            auditRiskEvaluator = new AuditRiskEvaluator();
        }
        return auditRiskEvaluator;
    }

    private void insertQuietly(AuditLog logEntry) {
        try {
            if (outboxService != null) {
                outboxService.audit(logEntry);
                return;
            }
            auditLogMapper.insert(logEntry);
            if (auditLogEventPublisher != null) {
                auditLogEventPublisher.publishIfImportant(logEntry);
            }
        } catch (Exception e) {
            log.warn("Failed to persist audit event: failures={}, error={}",
                    auditWriteFailures.incrementAndGet(), e.getClass().getSimpleName());
        }
    }

    private record UserSnapshot(Long userId, String username) {
        static UserSnapshot empty() {
            return new UserSnapshot(null, null);
        }

        boolean hasUser() {
            return userId != null;
        }
    }
}
