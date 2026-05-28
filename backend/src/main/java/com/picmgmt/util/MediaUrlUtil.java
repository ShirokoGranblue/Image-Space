package com.picmgmt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MediaUrlUtil {

    private MediaUrlUtil() {
    }

    public static String withVersion(String url, String versionSource) {
        if (url == null || url.isBlank() || versionSource == null || versionSource.isBlank()) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "v=" + versionToken(versionSource);
    }

    public static String imageDownloadUrl(Long imageId, String storageKey) {
        if (imageId == null) {
            return null;
        }
        return withVersion("/api/image/download/" + imageId, storageKey);
    }

    public static String userMediaUrl(String type, Long userId, String storageKey) {
        if (type == null || type.isBlank() || userId == null) {
            return null;
        }
        return withVersion("/api/user/" + type + "/" + userId, storageKey);
    }

    private static String versionToken(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder token = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                token.append(String.format("%02x", hash[i]));
            }
            return token.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toUnsignedString(value.hashCode(), 36);
        }
    }
}
