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
import com.picmgmt.cache.BloomFilterService;
import com.picmgmt.config.GoogleJwtVerifier;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
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
    private final StringRedisTemplate redisTemplate;
    private final BloomFilterService bloomFilterService;

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

    private final GoogleJwtVerifier googleJwtVerifier = new GoogleJwtVerifier();

    public OAuthServiceImpl(UserMapper userMapper, StorageService storageService, StringRedisTemplate redisTemplate, BloomFilterService bloomFilterService) {
        this.userMapper = userMapper;
        this.storageService = storageService;
        this.redisTemplate = redisTemplate;
        this.bloomFilterService = bloomFilterService;
    }

    @Override
    public String getAuthorizeUrl(String provider, String baseUrl) {
        String state = AuthStateUtils.createState();
        redisTemplate.opsForValue().set("oauth:domain:" + state, baseUrl, Duration.ofMinutes(10));
        AuthRequest authRequest = buildAuthRequest(provider);
        return authRequest.authorize(state);
    }

    @Override
    public OAuthResult handleCallback(String provider, String code, String state, String baseUrl) {
        AuthRequest authRequest = buildAuthRequest(provider);

        String redirectDomain = null;
        try {
            redirectDomain = redisTemplate.opsForValue().get("oauth:domain:" + state);
            if (redirectDomain != null && !redirectDomain.isBlank()) {
                redisTemplate.delete("oauth:domain:" + state);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable during {} OAuth callback: {}", provider, e.getMessage());
        }
        if (redirectDomain == null || redirectDomain.isBlank()) {
            redirectDomain = baseUrl;
        }

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

        User user = null;
        if (email != null && !email.isEmpty()) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user != null && (user.getDeleted() == null || user.getDeleted() == 0)) {
                bindOAuthUsername(user, provider, oauthUsername);
                StpUtil.login(user.getId());
                log.info("{} OAuth login: email match, user {}", provider, user.getUsername());
                return new OAuthResult(StpUtil.getTokenValue(), redirectDomain);
            }
        }

        user = new User();
        String username = provider + "_" + (oauthUsername != null ? oauthUsername : UUID.randomUUID().toString().substring(0, 8));
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
        }
        user.setUuid(java.util.UUID.randomUUID().toString());
        user.setUsername(username);
        user.setDisplayName(nickname != null ? nickname : oauthUsername);
        user.setPassword(BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt()));
        user.setRole("user");
        user.setEmail(email);
        userMapper.insert(user);
        bloomFilterService.addUser(user.getId());
        bindOAuthUsername(user, provider, oauthUsername);

        String avatarKey = downloadAndUploadAvatar(avatarUrl, user.getId());
        if (avatarKey != null) {
            user.setAvatarKey(avatarKey);
            userMapper.updateById(user);
        }

        StpUtil.login(user.getId());
        log.info("{} OAuth login: auto-registered user {}", provider, username);
        return new OAuthResult(StpUtil.getTokenValue(), redirectDomain);
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
            String objectKey = "avatars/oauth_" + userId + "." + ext;
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
                    .build(), googleJwtVerifier);
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
     * Google: parse ID token locally, verify JWT signature against Google's public keys,
     * then validate claims. Falls back to userinfo API on any verification failure.
     */
    static class IdTokenGoogleRequest extends AuthGoogleRequest {
        private final GoogleJwtVerifier verifier;

        IdTokenGoogleRequest(AuthConfig config, GoogleJwtVerifier verifier) {
            super(config);
            this.verifier = verifier;
        }

        @Override
        protected AuthUser getUserInfo(AuthToken authToken) {
            String idToken = authToken.getIdToken();
            if (StrUtil.isNotEmpty(idToken)) {
                // Verify JWT signature and claims before trusting the payload
                if (!verifier.verify(idToken, this.config.getClientId())) {
                    log.warn("Google ID token: verification failed, falling back to userinfo API");
                    return super.getUserInfo(authToken);
                }

                try {
                    String[] parts = idToken.split("\\.");
                    String payload = StrUtil.utf8Str(Base64.decode(parts[1]));
                    JSONObject claims = JSONUtil.parseObj(payload);

                    return AuthUser.builder()
                            .uuid(claims.getStr("sub"))
                            .username(claims.getStr("email"))
                            .nickname(claims.getStr("name"))
                            .email(claims.getStr("email"))
                            .avatar(claims.getStr("picture"))
                            .token(authToken)
                            .source("GOOGLE")
                            .build();
                } catch (Exception e) {
                    log.warn("Failed to parse Google ID token payload, falling back to userinfo API: {}", e.getMessage());
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
