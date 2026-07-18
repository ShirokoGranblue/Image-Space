package com.picmgmt.service.oauth;

import cn.hutool.crypto.digest.DigestUtil;

import java.text.Normalizer;
import java.util.function.Predicate;

public final class OAuthUsernameGenerator {

    private static final int MAX_USERNAME_LENGTH = 50;

    private OAuthUsernameGenerator() {
    }

    public static String createBaseUsername(
            String provider,
            String nickname,
            String providerUsername,
            String email,
            String providerUserId
    ) {
        String normalizedNickname = normalize(nickname);
        if (!normalizedNickname.isBlank()) {
            return normalizedNickname;
        }

        if (providerUsername != null && !providerUsername.contains("@")) {
            String normalizedProviderUsername = normalize(providerUsername);
            if (!normalizedProviderUsername.isBlank()) {
                return normalizedProviderUsername;
            }
        }

        String emailCandidate = firstNonBlank(email, providerUsername);
        if (emailCandidate != null && emailCandidate.contains("@")) {
            String normalizedEmailPrefix = normalize(emailCandidate.substring(0, emailCandidate.indexOf('@')));
            if (!normalizedEmailPrefix.isBlank()) {
                return normalizedEmailPrefix;
            }
        }

        String normalizedProvider = normalize(provider);
        if (normalizedProvider.isBlank()) {
            normalizedProvider = "oauth";
        }
        String stableSubject = normalizedProvider + ":" + (providerUserId == null ? "unknown" : providerUserId);
        return truncate(normalizedProvider + "_" + DigestUtil.sha256Hex(stableSubject).substring(0, 8));
    }

    public static String ensureUnique(String baseUsername, Predicate<String> usernameExists) {
        String base = truncate(baseUsername);
        if (!usernameExists.test(base)) {
            return base;
        }

        for (int suffix = 2; ; suffix++) {
            String suffixText = "_" + suffix;
            String candidate = truncate(base, MAX_USERNAME_LENGTH - suffixText.length()) + suffixText;
            if (!usernameExists.test(candidate)) {
                return candidate;
            }
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .replaceAll("[^\\p{L}\\p{N}_-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[_-]+|[_-]+$", "");
        return truncate(normalized);
    }

    private static String truncate(String value) {
        return truncate(value, MAX_USERNAME_LENGTH);
    }

    private static String truncate(String value, int maxCodePoints) {
        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }
}
