package com.picmgmt.service.oauth.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.picmgmt.config.MicrosoftOAuthProperties;
import com.picmgmt.config.OAuthPooledHttp;
import com.picmgmt.dto.oauth.MicrosoftTokenResponse;
import com.picmgmt.dto.oauth.MicrosoftUserInfo;
import com.picmgmt.service.oauth.MicrosoftOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Microsoft OAuth2 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MicrosoftOAuthServiceImpl implements MicrosoftOAuthService {

    private final MicrosoftOAuthProperties properties;

    @Override
    public String buildAuthorizeUrl(String state) {
        String encodedRedirectUri = URLEncoder.encode(properties.getRedirectUri(), StandardCharsets.UTF_8);
        String encodedScope = URLEncoder.encode(properties.getScope(), StandardCharsets.UTF_8);

        return properties.getAuthorizeUrl()
                + "?client_id=" + URLEncoder.encode(properties.getClientId(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&redirect_uri=" + encodedRedirectUri
                + "&response_mode=query"
                + "&scope=" + encodedScope
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
    }

    @Override
    public MicrosoftTokenResponse exchangeCodeForToken(String code) {
        OkHttpClient client = OAuthPooledHttp.getClient();
        if (client == null) {
            throw new RuntimeException("HTTP client not available");
        }

        FormBody body = new FormBody.Builder()
                .add("client_id", properties.getClientId())
                .add("client_secret", properties.getClientSecret())
                .add("code", code)
                .add("redirect_uri", properties.getRedirectUri())
                .add("grant_type", "authorization_code")
                .add("scope", properties.getScope())
                .build();

        Request request = new Request.Builder()
                .url(properties.getTokenUrl())
                .post(body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String bodyStr = responseBody != null ? responseBody.string() : "";

            if (!response.isSuccessful()) {
                log.error("Microsoft token exchange failed: HTTP {} - {}", response.code(),
                        maskSensitiveInfo(bodyStr));
                throw new RuntimeException("Microsoft token exchange failed: HTTP " + response.code());
            }

            JSONObject json = JSONUtil.parseObj(bodyStr);

            // 检查是否有错误
            if (json.containsKey("error")) {
                log.error("Microsoft token exchange error: {} - {}", json.getStr("error"),
                        json.getStr("error_description", ""));
                throw new RuntimeException("Microsoft token error: " + json.getStr("error"));
            }

            MicrosoftTokenResponse tokenResponse = new MicrosoftTokenResponse();
            tokenResponse.setTokenType(json.getStr("token_type"));
            tokenResponse.setScope(json.getStr("scope"));
            tokenResponse.setExpiresIn(json.getInt("expires_in"));
            tokenResponse.setAccessToken(json.getStr("access_token"));
            tokenResponse.setRefreshToken(json.getStr("refresh_token"));
            tokenResponse.setIdToken(json.getStr("id_token"));

            log.info("Microsoft token exchange successful, token_type={}, expires_in={}",
                    tokenResponse.getTokenType(), tokenResponse.getExpiresIn());

            return tokenResponse;
        } catch (IOException e) {
            log.error("Microsoft token exchange IO error: {}", e.getMessage());
            throw new RuntimeException("Microsoft token exchange failed", e);
        }
    }

    @Override
    public MicrosoftUserInfo getMicrosoftUserInfo(String accessToken) {
        OkHttpClient client = OAuthPooledHttp.getClient();
        if (client == null) {
            throw new RuntimeException("HTTP client not available");
        }

        Request request = new Request.Builder()
                .url(properties.getUserInfoUrl())
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String bodyStr = responseBody != null ? responseBody.string() : "";

            if (!response.isSuccessful()) {
                log.error("Microsoft Graph /me failed: HTTP {} - {}", response.code(),
                        maskSensitiveInfo(bodyStr));
                throw new RuntimeException("Microsoft user info failed: HTTP " + response.code());
            }

            JSONObject json = JSONUtil.parseObj(bodyStr);

            MicrosoftUserInfo userInfo = new MicrosoftUserInfo();
            userInfo.setId(json.getStr("id"));
            userInfo.setDisplayName(json.getStr("displayName"));
            userInfo.setMail(json.getStr("mail"));
            userInfo.setUserPrincipalName(json.getStr("userPrincipalName"));

            if (userInfo.getId() == null || userInfo.getId().isBlank()) {
                log.error("Microsoft user info missing id");
                throw new RuntimeException("Microsoft user info missing id");
            }

            log.info("Microsoft user info retrieved: id={}, displayName={}",
                    userInfo.getId(), userInfo.getDisplayName());

            return userInfo;
        } catch (IOException e) {
            log.error("Microsoft Graph /me IO error: {}", e.getMessage());
            throw new RuntimeException("Microsoft user info failed", e);
        }
    }

    /**
     * 脱敏敏感信息：移除可能包含的 token、密钥等字段，仅保留错误类型和描述。
     * 避免将 access_token、refresh_token、id_token、client_secret 等写入日志。
     */
    private String maskSensitiveInfo(String text) {
        if (text == null) return null;
        try {
            JSONObject json = JSONUtil.parseObj(text);
            // 仅保留 error 相关字段，移除所有 token 和敏感字段
            JSONObject safe = new JSONObject();
            if (json.containsKey("error")) safe.set("error", json.getStr("error"));
            if (json.containsKey("error_description")) safe.set("error_description", json.getStr("error_description"));
            if (json.containsKey("error_codes")) safe.set("error_codes", json.get("error_codes"));
            if (json.containsKey("timestamp")) safe.set("timestamp", json.get("timestamp"));
            if (json.containsKey("trace_id")) safe.set("trace_id", json.getStr("trace_id"));
            if (json.containsKey("correlation_id")) safe.set("correlation_id", json.getStr("correlation_id"));
            if (!safe.isEmpty()) {
                return safe.toString();
            }
        } catch (Exception ignored) {
            // 不是 JSON，按纯文本处理
        }
        // 纯文本：截断到 100 字符并移除可能的 token 片段
        if (text.length() > 100) {
            return text.substring(0, 100) + "...[truncated]";
        }
        return text;
    }
}
