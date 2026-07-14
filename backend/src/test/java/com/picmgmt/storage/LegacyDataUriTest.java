package com.picmgmt.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDataUriTest {

    @Test
    void parseAcceptsSupportedImageData() {
        String encoded = Base64.getEncoder().encodeToString("image".getBytes(StandardCharsets.UTF_8));

        LegacyDataUri result = LegacyDataUri.parse("data:image/jpg;base64," + encoded).orElseThrow();

        assertEquals("image/jpeg", result.contentType());
        assertArrayEquals("image".getBytes(StandardCharsets.UTF_8), result.bytes());
    }

    @Test
    void parseRejectsExternalUrlsUnsupportedTypesAndInvalidBase64() {
        assertTrue(LegacyDataUri.parse("https://example.com/avatar.png").isEmpty());
        assertTrue(LegacyDataUri.parse("data:text/plain;base64,dGV4dA==").isEmpty());
        assertTrue(LegacyDataUri.parse("data:image/png;base64,%%%").isEmpty());
    }
}
