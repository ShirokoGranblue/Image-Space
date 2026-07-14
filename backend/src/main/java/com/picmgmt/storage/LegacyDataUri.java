package com.picmgmt.storage;

import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record LegacyDataUri(String contentType, byte[] bytes) {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "image/jpeg", "image/jpeg",
            "image/jpg", "image/jpeg",
            "image/png", "image/png",
            "image/webp", "image/webp",
            "image/gif", "image/gif"
    );

    public static Optional<LegacyDataUri> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        int commaIndex = value.indexOf(',');
        if (commaIndex <= 5) {
            return Optional.empty();
        }

        String header = value.substring(0, commaIndex).toLowerCase(Locale.ROOT);
        if (!header.startsWith("data:") || !header.endsWith(";base64")) {
            return Optional.empty();
        }

        String contentType = CONTENT_TYPES.get(header.substring(5, header.length() - 7));
        if (contentType == null) {
            return Optional.empty();
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(value.substring(commaIndex + 1));
            return bytes.length == 0
                    ? Optional.empty()
                    : Optional.of(new LegacyDataUri(contentType, bytes));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
