package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.service.OAuthService;
import com.picmgmt.storage.StorageService;
import com.xkcoding.http.config.HttpConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.enums.scope.AuthGithubScope;
import me.zhyd.oauth.enums.scope.AuthGoogleScope;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthGoogleRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final UserMapper userMapper;
    private final StorageService storageService;

    @Value("${oauth.github.client-id}")
    private String githubClientId;
    @Value("${oauth.github.client-secret}")
    private String githubClientSecret;
    @Value("${oauth.github.redirect-uri}")
    private String githubRedirectUri;

    @Value("${oauth.google.client-id}")
    private String googleClientId;
    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;
    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${oauth.timeout:30000}")
    private int timeout;

    @Value("${oauth.proxy.enabled:false}")
    private boolean proxyEnabled;
    @Value("${oauth.proxy.host:}")
    private String proxyHost;
    @Value("${oauth.proxy.port:0}")
    private int proxyPort;

    @Override
    public String getAuthorizeUrl(String provider) {
        AuthRequest authRequest = buildAuthRequest(provider);
        return authRequest.authorize(AuthStateUtils.createState());
    }

    @Override
    public String handleCallback(String provider, String code, String state) {
        AuthRequest authRequest = buildAuthRequest(provider);
        AuthResponse<AuthUser> response = authRequest.login(AuthCallback.builder()
                .code(code)
                .state(state)
                .build());

        if (!response.ok()) {
            log.error("{} OAuth failed: {} {}", provider, response.getCode(), response.getMsg());
            throw new RuntimeException(provider + "授权失败: " + response.getMsg());
        }

        AuthUser authUser = response.getData();
        String oauthUsername = authUser.getUsername();
        String email = authUser.getEmail();
        String avatarUrl = authUser.getAvatar();
        String nickname = authUser.getNickname();

        // GitHub /user endpoint returns null email for private emails
        if (StrUtil.isEmpty(email) && "github".equals(provider)) {
            email = fetchGithubPrimaryEmail(authUser.getToken().getAccessToken());
            log.info("Fetched GitHub primary email: {}", email);
        }

        // Email-first: match by OAuth email to bind with existing account
        User user = null;
        if (email != null && !email.isEmpty()) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user != null && (user.getDeleted() == null || user.getDeleted() == 0)) {
                bindOAuthUsername(user, provider, oauthUsername);
                StpUtil.login(user.getId());
                log.info("{} OAuth login: email match, user {}", provider, user.getUsername());
                return StpUtil.getTokenValue();
            }
        }

        // Auto-register new user
        user = new User();
        String username = provider + "_" + (oauthUsername != null ? oauthUsername : UUID.randomUUID().toString().substring(0, 8));
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
        }
        user.setUsername(username);
        user.setDisplayName(nickname != null ? nickname : oauthUsername);
        user.setPassword(BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt()));
        user.setRole("user");
        user.setEmail(email);
        bindOAuthUsername(user, provider, oauthUsername);
        userMapper.insert(user);

        String avatarKey = downloadAndUploadAvatar(avatarUrl, user.getId());
        if (avatarKey != null) {
            user.setAvatarKey(avatarKey);
            userMapper.updateById(user);
        }

        StpUtil.login(user.getId());
        log.info("{} OAuth login: auto-registered user {}", provider, username);
        return StpUtil.getTokenValue();
    }

    private void bindOAuthUsername(User user, String provider, String oauthUsername) {
        if ("github".equals(provider)) {
            if (user.getGithubUsername() == null) {
                user.setGithubUsername(oauthUsername);
                userMapper.updateById(user);
            }
        }
        // Google doesn't have a separate username field; email binding is sufficient
    }

    private String fetchGithubPrimaryEmail(String accessToken) {
        try {
            String body = HttpRequest.get("https://api.github.com/user/emails")
                    .header("Authorization", "token " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .execute()
                    .body();
            JSONArray emails = JSONUtil.parseArray(body);
            for (int i = 0; i < emails.size(); i++) {
                JSONObject emailObj = emails.getJSONObject(i);
                if (emailObj.getBool("primary", false) && emailObj.getBool("verified", false)) {
                    return emailObj.getStr("email");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch GitHub emails: {}", e.getMessage());
        }
        return null;
    }

    private String downloadAndUploadAvatar(String avatarUrl, Long userId) {
        if (avatarUrl == null || avatarUrl.isEmpty()) return null;
        try {
            byte[] bytes = HttpUtil.downloadBytes(avatarUrl);
            String ext = FileUtil.extName(avatarUrl);
            if (ext == null || ext.length() > 5) ext = "png";
            String objectKey = userId + "/oauth_avatar." + ext;
            String mimeType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
            storageService.upload("avatars", objectKey, bytes, mimeType);
            return storageService.getAccessUrl("avatars", objectKey);
        } catch (Exception e) {
            log.warn("Failed to download avatar for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private AuthRequest buildAuthRequest(String provider) {
        HttpConfig httpConfig = HttpConfig.builder()
                .timeout(timeout)
                .proxy(proxyEnabled ? new Proxy(Proxy.Type.HTTP,
                        new InetSocketAddress(proxyHost, proxyPort)) : Proxy.NO_PROXY)
                .build();

        if ("google".equals(provider)) {
            return new AuthGoogleRequest(AuthConfig.builder()
                    .clientId(googleClientId)
                    .clientSecret(googleClientSecret)
                    .redirectUri(googleRedirectUri)
                    .scopes(List.of(AuthGoogleScope.USER_EMAIL.getScope(),
                            AuthGoogleScope.USER_PROFILE.getScope(),
                            AuthGoogleScope.USER_OPENID.getScope()))
                    .httpConfig(httpConfig)
                    .build());
        }

        return new AuthGithubRequest(AuthConfig.builder()
                .clientId(githubClientId)
                .clientSecret(githubClientSecret)
                .redirectUri(githubRedirectUri)
                .scopes(List.of(AuthGithubScope.USER.getScope(),
                        AuthGithubScope.USER_EMAIL.getScope()))
                .httpConfig(httpConfig)
                .build());
    }
}
