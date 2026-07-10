package com.picmgmt.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogSanitizerTest {

    private final AuditLogSanitizer sanitizer = new AuditLogSanitizer(new ObjectMapper());

    @Test
    void sanitizeTextMasksSensitiveJsonValues() {
        String text = """
                {"password":"secret","email":"alice@example.com","phone":"13812345678","safe":"ok"}
                """;

        String result = sanitizer.sanitizeText(text, 1000);

        assertTrue(result.contains("\"password\":\"[FILTERED]\""));
        assertTrue(result.contains("\"email\":\"[FILTERED]\""));
        assertTrue(result.contains("\"phone\":\"[FILTERED]\""));
        assertTrue(result.contains("\"safe\":\"ok\""));
        assertFalse(result.contains("secret"));
        assertFalse(result.contains("alice@example.com"));
        assertFalse(result.contains("13812345678"));
    }

    @Test
    void toSanitizedJsonMasksNestedSensitiveValues() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("authorization", "Bearer token");
        payload.put("profile", Map.of(
                "email", "bob@example.com",
                "displayName", "Bob"
        ));

        String result = sanitizer.toSanitizedJson(payload, 1000);

        assertTrue(result.contains("\"authorization\":\"[FILTERED]\""));
        assertTrue(result.contains("\"email\":\"[FILTERED]\""));
        assertTrue(result.contains("\"displayName\":\"Bob\""));
        assertFalse(result.contains("Bearer token"));
        assertFalse(result.contains("bob@example.com"));
    }

    @Test
    void sanitizeTextTruncatesNonJsonTextAfterMaskingTokenLikeValues() {
        String result = sanitizer.sanitizeText("token=abc123&message=" + "x".repeat(100), 32);

        assertTrue(result.length() <= 32);
        assertFalse(result.contains("abc123"));
    }
}
