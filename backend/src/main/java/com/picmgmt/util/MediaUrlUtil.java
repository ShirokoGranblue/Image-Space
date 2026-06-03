package com.picmgmt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MediaUrlUtil {

    @Value("${storage.r2.public-url:https://cdn.image-space.app}")
    private String storagePublicUrl;

    public String getPublicUrl() {
        return storagePublicUrl.replaceAll("/$", "");
    }

    public static String withVersion(String url, String versionSource) {
        if (url == null || url.isBlank() || versionSource == null || versionSource.isBlank()) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "v=" + versionToken(versionSource);
    }

    public String imageDownloadUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        String url = getPublicUrl() + "/" + storageKey.replaceAll("^/", "");
        return withVersion(url, storageKey);
    }

    public String userMediaUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        String url = getPublicUrl() + "/" + storageKey.replaceAll("^/", "");
        return withVersion(url, storageKey);
    }

    public String userAvatarUrl(String userUuid, String storageKey) {
        return userMediaEndpoint("avatar", userUuid, storageKey);
    }

    public String userBackgroundUrl(String userUuid, String storageKey) {
        return userMediaEndpoint("background", userUuid, storageKey);
    }

    private String userMediaEndpoint(String kind, String userUuid, String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        if (userUuid == null || userUuid.isBlank()) {
            return userMediaUrl(storageKey);
        }
        return withVersion("/api/user/" + kind + "/" + userUuid.trim(), storageKey);
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
