package com.picmgmt.config;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifies Google ID Token JWT signatures using Google's published RSA public keys.
 * Keys are fetched from https://www.googleapis.com/oauth2/v3/certs and cached for 1 hour.
 */
@Slf4j
public class GoogleJwtVerifier {

    private static final String CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final long CACHE_TTL_MS = 3_600_000; // 1 hour
    private static final String EXPECTED_ISSUER = "https://accounts.google.com";
    private static final String ALT_ISSUER = "accounts.google.com";

    private volatile Map<String, RSAPublicKey> cachedKeys;
    private volatile long cacheTimestamp;

    /**
     * Verify a Google ID Token's signature and standard claims (iss, aud, exp, iat).
     *
     * @param idToken  the raw JWT string from Google
     * @param clientId the expected audience (OAuth client ID)
     * @return true if the token is cryptographically valid and claims are correct
     */
    public boolean verify(String idToken, String clientId) {
        if (StrUtil.isBlank(idToken)) return false;

        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            log.warn("Google ID token: not a valid JWT (expected 3 parts, got {})", parts.length);
            return false;
        }

        // Parse header to extract kid and alg
        JSONObject header;
        try {
            header = JSONUtil.parseObj(StrUtil.utf8Str(Base64.decode(parts[0])));
        } catch (Exception e) {
            log.warn("Google ID token: failed to parse JWT header: {}", e.getMessage());
            return false;
        }

        String kid = header.getStr("kid");
        String alg = header.getStr("alg");
        if (!"RS256".equals(alg)) {
            log.warn("Google ID token: unexpected algorithm {}, expected RS256", alg);
            return false;
        }
        if (StrUtil.isBlank(kid)) {
            log.warn("Google ID token: missing kid in header");
            return false;
        }

        // Verify cryptographic signature
        RSAPublicKey publicKey = getPublicKey(kid);
        if (publicKey == null) {
            log.warn("Google ID token: no public key found for kid={}", kid);
            return false;
        }

        byte[] signedBytes = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8);
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(signedBytes);
            byte[] signatureBytes = Base64.decode(parts[2]);
            if (!sig.verify(signatureBytes)) {
                log.warn("Google ID token: signature verification failed for kid={}", kid);
                return false;
            }
        } catch (Exception e) {
            log.error("Google ID token: signature verification error: {}", e.getMessage());
            return false;
        }

        // Verify claims
        JSONObject payload;
        try {
            payload = JSONUtil.parseObj(StrUtil.utf8Str(Base64.decode(parts[1])));
        } catch (Exception e) {
            log.warn("Google ID token: failed to parse payload: {}", e.getMessage());
            return false;
        }

        long now = System.currentTimeMillis() / 1000;
        String iss = payload.getStr("iss");
        String aud = payload.getStr("aud");
        Long exp = payload.getLong("exp");
        Long iat = payload.getLong("iat");

        if (!EXPECTED_ISSUER.equals(iss) && !ALT_ISSUER.equals(iss)) {
            log.warn("Google ID token: invalid iss={}", iss);
            return false;
        }
        if (!clientId.equals(aud)) {
            log.warn("Google ID token: aud mismatch, expected {} got {}", clientId, aud);
            return false;
        }
        if (exp == null || exp <= now) {
            log.warn("Google ID token: expired (exp={}, now={})", exp, now);
            return false;
        }
        if (iat != null && iat > now + 300) {
            log.warn("Google ID token: iat too far in future (iat={}, now={})", iat, now);
            return false;
        }

        return true;
    }

    private RSAPublicKey getPublicKey(String kid) {
        Map<String, RSAPublicKey> keys = refreshKeysIfNeeded();
        return keys.get(kid);
    }

    private Map<String, RSAPublicKey> refreshKeysIfNeeded() {
        if (cachedKeys != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
            return cachedKeys;
        }
        synchronized (this) {
            if (cachedKeys != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
                return cachedKeys;
            }
            try {
                String response = HttpUtil.get(CERTS_URL, 10_000);
                JSONObject json = JSONUtil.parseObj(response);
                JSONArray keys = json.getJSONArray("keys");
                Map<String, RSAPublicKey> newKeys = new ConcurrentHashMap<>();
                for (int i = 0; i < keys.size(); i++) {
                    JSONObject key = keys.getJSONObject(i);
                    String keyKid = key.getStr("kid");
                    String kty = key.getStr("kty");
                    if (!"RSA".equals(kty)) continue;
                    RSAPublicKey rsaKey = buildRSAPublicKey(key.getStr("n"), key.getStr("e"));
                    newKeys.put(keyKid, rsaKey);
                }
                if (newKeys.isEmpty()) {
                    log.error("Google ID token: no RSA keys found in JWK set");
                    return cachedKeys != null ? cachedKeys : Map.of();
                }
                cachedKeys = newKeys;
                cacheTimestamp = System.currentTimeMillis();
                log.info("Google ID token: refreshed {} public keys from Google", newKeys.size());
            } catch (Exception e) {
                log.error("Google ID token: failed to fetch Google JWK keys: {}", e.getMessage());
                return cachedKeys != null ? cachedKeys : Map.of();
            }
        }
        return cachedKeys;
    }

    private RSAPublicKey buildRSAPublicKey(String modulusBase64, String exponentBase64) {
        try {
            byte[] modulusBytes = Base64.decode(modulusBase64);
            byte[] exponentBytes = Base64.decode(exponentBase64);

            // BigInteger constructor expects unsigned magnitude; Google uses
            // unsigned big-endian encoding for both modulus and exponent.
            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build RSA public key from JWK", e);
        }
    }
}
