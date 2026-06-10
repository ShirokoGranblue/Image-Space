package com.picmgmt.controller.oauth;

import cn.hutool.core.util.StrUtil;
import com.picmgmt.config.MicrosoftOAuthProperties;
import com.picmgmt.dto.oauth.MicrosoftTokenResponse;
import com.picmgmt.dto.oauth.MicrosoftUserInfo;
import com.picmgmt.dto.oauth.OAuthLoginResult;
import com.picmgmt.service.oauth.MicrosoftOAuthService;
import com.picmgmt.service.oauth.OAuthLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * Microsoft OAuth2 控制器
 *
 * 实现 Microsoft OAuth2 Authorization Code Flow：
 * 1. /login - 生成 state，重定向到 Microsoft 授权页面
 * 2. /callback - 处理回调，交换 token，获取用户信息，登录/注册用户
 *
 * 安全说明：
 * - 登录成功后通过一次性 code 传递 token（而非直接在 URL 暴露 token），
 *   前端用 code 换取真正的 sa-token，避免 token 暴露在浏览器历史和日志中。
 */
@Tag(name = "Microsoft OAuth2")
@Slf4j
@RestController
@RequestMapping("/oauth/microsoft")
@RequiredArgsConstructor
public class MicrosoftOAuthController {

    private final MicrosoftOAuthService microsoftOAuthService;
    private final OAuthLoginService oAuthLoginService;
    private final MicrosoftOAuthProperties properties;
    private final StringRedisTemplate redisTemplate;

    private static final String STATE_KEY_PREFIX = "oauth:microsoft:state:";
    private static final String BASE_URL_KEY_PREFIX = "oauth:microsoft:baseurl:";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    /**
     * 生成安全的随机 state
     */
    private String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return UUID.randomUUID().toString() + "-" + bytesToHex(bytes).substring(0, 16);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Operation(summary = "发起 Microsoft OAuth2 登录")
    @GetMapping("/login")
    public void login(
            @RequestParam(required = false) String baseUrl,
            HttpServletResponse response) throws IOException {

        // 生成 state
        String state = generateState();

        // 存储 state 到 Redis，用于 CSRF 防护
        redisTemplate.opsForValue().set(STATE_KEY_PREFIX + state, "1", STATE_TTL);

        // 存储前端 baseUrl，用于回调后重定向
        if (baseUrl != null && !baseUrl.isBlank()) {
            redisTemplate.opsForValue().set(BASE_URL_KEY_PREFIX + state, baseUrl, STATE_TTL);
        }

        // 构建授权 URL
        String authorizeUrl = microsoftOAuthService.buildAuthorizeUrl(state);

        log.info("Microsoft OAuth login initiated, state={}", state.substring(0, 8) + "...");

        // 重定向到 Microsoft
        response.sendRedirect(authorizeUrl);
    }

    @Operation(summary = "Microsoft OAuth2 回调")
    @GetMapping("/callback")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, name = "error_description") String errorDescription,
            HttpServletResponse response) throws IOException {

        // 确定前端重定向地址
        String frontendBaseUrl = null;

        try {
            // 获取存储的 baseUrl
            if (state != null) {
                String storedBaseUrl = redisTemplate.opsForValue().get(BASE_URL_KEY_PREFIX + state);
                if (storedBaseUrl != null) {
                    frontendBaseUrl = storedBaseUrl;
                    redisTemplate.delete(BASE_URL_KEY_PREFIX + state);
                }
            }
        } catch (Exception e) {
            log.warn("Redis unavailable during Microsoft OAuth callback: {}", e.getMessage());
        }

        // 默认使用配置的失败 URL 或前端基础 URL
        String failureUrl = getFailureUrl(frontendBaseUrl);

        // 1. 检查 Microsoft 返回的错误
        if (error != null && !error.isBlank()) {
            log.warn("Microsoft OAuth error: {} - {}", error, errorDescription);
            redirectWithError(response, failureUrl, "oauth_error");
            return;
        }

        // 2. 检查 code
        if (code == null || code.isBlank()) {
            log.warn("Microsoft OAuth callback missing code");
            redirectWithError(response, failureUrl, "missing_code");
            return;
        }

        // 3. 检查 state
        if (state == null || state.isBlank()) {
            log.warn("Microsoft OAuth callback missing state");
            redirectWithError(response, failureUrl, "missing_state");
            return;
        }

        // 4. 校验 state
        String stateKey = STATE_KEY_PREFIX + state;
        Boolean stateExists = redisTemplate.hasKey(stateKey);
        if (stateExists == null || !stateExists) {
            log.warn("Microsoft OAuth callback invalid/expired state");
            redirectWithError(response, failureUrl, "state_invalid");
            return;
        }

        // 删除 state，防止重复使用
        redisTemplate.delete(stateKey);

        try {
            // 5. 交换 token
            MicrosoftTokenResponse tokenResponse = microsoftOAuthService.exchangeCodeForToken(code);

            // 6. 获取用户信息
            MicrosoftUserInfo userInfo = microsoftOAuthService.getMicrosoftUserInfo(tokenResponse.getAccessToken());

            // 7. 登录或注册用户
            OAuthLoginResult loginResult = oAuthLoginService.loginOrRegisterByMicrosoft(userInfo);

            // 8. 生成一次性 code，避免在 URL 中直接暴露 token
            //    这与现有的 GitHub/Google OAuth 流程保持一致的安全模式
            String oneTimeCode = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(
                    "oauth:code:" + oneTimeCode,
                    loginResult.getTokenValue(),
                    Duration.ofSeconds(60));

            // 9. 重定向到前端成功页面
            String successUrl = getSuccessUrl(frontendBaseUrl);
            String redirectUrl = successUrl + "?oauth_code=" +
                    URLEncoder.encode(oneTimeCode, StandardCharsets.UTF_8);

            log.info("Microsoft OAuth login successful, userId={}, newlyCreated={}",
                    loginResult.getUserId(), loginResult.getNewlyCreated());

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Microsoft OAuth callback processing failed: {}", e.getMessage(), e);
            redirectWithError(response, failureUrl, "login_failed");
        }
    }

    /**
     * 获取成功重定向 URL
     */
    private String getSuccessUrl(String frontendBaseUrl) {
        if (properties.getFrontendSuccessUrl() != null && !properties.getFrontendSuccessUrl().isBlank()) {
            return properties.getFrontendSuccessUrl();
        }
        // 默认使用前端 baseUrl + 路径
        String base = frontendBaseUrl != null ? frontendBaseUrl : "http://localhost:3000";
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/login";
    }

    /**
     * 获取失败重定向 URL
     */
    private String getFailureUrl(String frontendBaseUrl) {
        if (properties.getFrontendFailureUrl() != null && !properties.getFrontendFailureUrl().isBlank()) {
            return properties.getFrontendFailureUrl();
        }
        // 默认使用前端 baseUrl + /login
        String base = frontendBaseUrl != null ? frontendBaseUrl : "http://localhost:3000";
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/login";
    }

    /**
     * 重定向到错误页面
     */
    private void redirectWithError(HttpServletResponse response, String baseUrl, String errorCode) throws IOException {
        String separator = baseUrl.contains("?") ? "&" : "?";
        String redirectUrl = baseUrl + separator + "oauthError=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }
}
