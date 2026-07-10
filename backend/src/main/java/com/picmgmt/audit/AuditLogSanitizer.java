package com.picmgmt.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.common.Result;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AuditLogSanitizer {

    public static final String FILTERED = "[FILTERED]";
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(password|oldPassword|newPassword|token|accessToken|refreshToken|authorization|cookie|phone|email|captcha|code)(\\s*[=:]\\s*)([^&\\s,}\\]]+)"
    );

    private final ObjectMapper objectMapper;

    public String toSanitizedJson(Object value, int maxLength) {
        try {
            Object sanitized = sanitizeValue("", value);
            return truncate(objectMapper.writeValueAsString(sanitized), maxLength);
        } catch (JsonProcessingException e) {
            return sanitizeText(String.valueOf(value), maxLength);
        }
    }

    public String sanitizeText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                Object parsed = objectMapper.readValue(trimmed, new TypeReference<Object>() {});
                return toSanitizedJson(parsed, maxLength);
            } catch (Exception ignored) {
                // Fall through to lightweight text masking.
            }
        }
        return truncate(KEY_VALUE_SECRET.matcher(text).replaceAll("$1$2" + FILTERED), maxLength);
    }

    public Object sanitizeValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveKey(key)) {
            return FILTERED;
        }
        if (value instanceof Result<?> result) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", result.getCode());
            map.put("message", result.getMessage());
            map.put("data", sanitizeValue("data", result.getData()));
            return map;
        }
        if (value instanceof MultipartFile file) {
            return fileMetadata(file);
        }
        if (value instanceof MultipartFile[] files) {
            List<Object> list = new ArrayList<>();
            for (MultipartFile file : files) {
                list.add(fileMetadata(file));
            }
            return list;
        }
        if (value instanceof ServletRequest || value instanceof ServletResponse
                || value instanceof BindingResult || value instanceof InputStream
                || value instanceof OutputStream) {
            return null;
        }
        if (value instanceof CharSequence || value instanceof Number
                || value instanceof Boolean || value instanceof Enum<?>) {
            return value;
        }
        if (value instanceof byte[]) {
            return "[BINARY]";
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String childKey = String.valueOf(entry.getKey());
                sanitized.put(childKey, sanitizeValue(childKey, entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : iterable) {
                sanitized.add(sanitizeValue(key, item));
            }
            return sanitized;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> sanitized = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                sanitized.add(sanitizeValue(key, Array.get(value, i)));
            }
            return sanitized;
        }
        try {
            Map<String, Object> map = objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
            return sanitizeValue(key, map);
        } catch (IllegalArgumentException ignored) {
            return sanitizeText(value.toString(), 1000);
        }
    }

    public boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return lower.contains("password")
                || lower.contains("token")
                || lower.contains("authorization")
                || lower.contains("cookie")
                || lower.contains("phone")
                || lower.contains("email")
                || lower.contains("captcha")
                || "code".equals(lower);
    }

    private Map<String, Object> fileMetadata(MultipartFile file) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("originalFilename", file.getOriginalFilename());
        metadata.put("contentType", file.getContentType());
        metadata.put("size", file.getSize());
        metadata.put("empty", file.isEmpty());
        return metadata;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
