package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picmgmt.config.OAuthPooledHttp;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.service.OAuthService;
import com.picmgmt.storage.StorageService;
import com.xkcoding.http.config.HttpConfig;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.enums.scope.AuthGithubScope;
import me.zhyd.oauth.enums.scope.AuthGoogleScope;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthGoogleRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
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

    public OAuthServiceImpl(UserMapper userMapper, StorageService storageService) {
        this.userMapper = userMapper;
        this.storageService = storageService;
    }

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
        userMapper.insert(user);
        bindOAuthUsername(user, provider, oauthUsername);

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
            return objectKey;
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
            return new IdTokenGoogleRequest(AuthConfig.builder()
                    .clientId(googleClientId)
                    .clientSecret(googleClientSecret)
                    .redirectUri(googleRedirectUri)
                    .scopes(List.of(AuthGoogleScope.USER_EMAIL.getScope(),
                            AuthGoogleScope.USER_PROFILE.getScope(),
                            AuthGoogleScope.USER_OPENID.getScope()))
                    .httpConfig(httpConfig)
                    .build());
        }

        return new ParallelGithubRequest(AuthConfig.builder()
                .clientId(githubClientId)
                .clientSecret(githubClientSecret)
                .redirectUri(githubRedirectUri)
                .scopes(List.of(AuthGithubScope.USER.getScope(),
                        AuthGithubScope.USER_EMAIL.getScope()))
                .httpConfig(httpConfig)
                .build());
    }

    /**
     * Google: parse ID token locally, skip the userinfo API call entirely.
     * Validates JWT claims (aud, iss, exp) before trusting the payload.
     */
    static class IdTokenGoogleRequest extends AuthGoogleRequest {
        IdTokenGoogleRequest(AuthConfig config) {
            super(config);
        }

        @Override
        protected AuthUser getUserInfo(AuthToken authToken) {
            String idToken = authToken.getIdToken();
            if (StrUtil.isNotEmpty(idToken)) {
                try {
                    String[] parts = idToken.split("\\.");
                    if (parts.length == 3) {
                        String payload = StrUtil.utf8Str(Base64.decode(parts[1]));
                        JSONObject claims = JSONUtil.parseObj(payload);

                        // Verify required OIDC claims before trusting the payload
                        long now = System.currentTimeMillis() / 1000;
                        String aud = claims.getStr("aud");
                        String iss = claims.getStr("iss");
                        Long exp = claims.getLong("exp");
                        Long iat = claims.getLong("iat");

                        if (!"https://accounts.google.com".equals(iss) && !"accounts.google.com".equals(iss)) {
                            log.warn("Google ID token: invalid iss={}", iss);
                            return super.getUserInfo(authToken);
                        }
                        if (!this.config.getClientId().equals(aud)) {
                            log.warn("Google ID token: aud mismatch, expected {} got {}", this.config.getClientId(), aud);
                            return super.getUserInfo(authToken);
                        }
                        if (exp == null || exp <= now) {
                            log.warn("Google ID token: expired or missing exp, exp={}", exp);
                            return super.getUserInfo(authToken);
                        }
                        if (iat != null && iat > now + 300) {
                            log.warn("Google ID token: iat in the future, iat={}", iat);
                            return super.getUserInfo(authToken);
                        }

                        return AuthUser.builder()
                                .uuid(claims.getStr("sub"))
                                .username(claims.getStr("email"))
                                .nickname(claims.getStr("name"))
                                .email(claims.getStr("email"))
                                .avatar(claims.getStr("picture"))
                                .token(authToken)
                                .source("GOOGLE")
                                .build();
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse Google ID token, falling back to userinfo API: {}", e.getMessage());
                }
            }
            return super.getUserInfo(authToken);
        }
    }

    /**
     * GitHub: fetch /user and /user/emails in parallel using the shared OkHttp connection pool.
     */
    static class ParallelGithubRequest extends AuthGithubRequest {
        private static final ExecutorService OAUTH_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "oauth-github-");
            t.setDaemon(true);
            return t;
        });

        ParallelGithubRequest(AuthConfig config) {
            super(config);
        }

        @Override
        protected AuthUser getUserInfo(AuthToken authToken) {
            OkHttpClient client = OAuthPooledHttp.getClient();
            if (client == null) {
                return super.getUserInfo(authToken);
            }

            String token = authToken.getAccessToken();
            CompletableFuture<JSONObject> userFuture = CompletableFuture.supplyAsync(() -> {
                Request req = new Request.Builder()
                        .url("https://api.github.com/user")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();
                try (Response resp = client.newCall(req).execute()) {
                    okhttp3.ResponseBody body = resp.body();
                    if (body == null) throw new java.io.IOException("Empty response body from /user");
                    return JSONUtil.parseObj(body.string());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, OAUTH_EXECUTOR);

            CompletableFuture<String> emailFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    Request req = new Request.Builder()
                            .url("https://api.github.com/user/emails")
                            .header("Authorization", "token " + token)
                            .header("Accept", "application/vnd.github.v3+json")
                            .build();
                    try (Response resp = client.newCall(req).execute()) {
                        okhttp3.ResponseBody body = resp.body();
                        if (body == null) return null;
                        JSONArray emails = JSONUtil.parseArray(body.string());
                        for (int i = 0; i < emails.size(); i++) {
                            JSONObject e = emails.getJSONObject(i);
                            if (e.getBool("primary", false) && e.getBool("verified", false)) {
                                return e.getStr("email");
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Failed to fetch GitHub emails: {}", ex.getMessage());
                }
                return null;
            }, OAUTH_EXECUTOR);

            try {
                JSONObject userJson = userFuture.join();
                String email = emailFuture.join();
                return AuthUser.builder()
                        .uuid(userJson.getStr("id"))
                        .username(userJson.getStr("login"))
                        .nickname(userJson.getStr("name"))
                        .email(email)
                        .avatar(userJson.getStr("avatar_url"))
                        .token(authToken)
                        .source("GITHUB")
                        .build();
            } catch (Exception e) {
                log.warn("Parallel GitHub fetch failed, falling back: {}", e.getMessage());
                return super.getUserInfo(authToken);
            }
        }
    }
}
