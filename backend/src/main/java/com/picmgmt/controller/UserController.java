package com.picmgmt.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.annotation.Audit;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.common.Result;
import com.picmgmt.dto.CodeLoginDTO;
import com.picmgmt.dto.LoginDTO;
import com.picmgmt.dto.RegisterDTO;
import com.picmgmt.dto.SendCodeDTO;
import com.picmgmt.entity.User;
import com.picmgmt.image.ImageUrlService;
import com.picmgmt.service.CaptchaService;
import com.picmgmt.service.OAuthService;
import com.picmgmt.service.TurnstileService;
import com.picmgmt.service.UserService;
import com.picmgmt.storage.LegacyDataUri;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.UserProfileVO;
import com.picmgmt.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;

@Tag(name = "用户模块")
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StorageService storageService;
    private final CaptchaService captchaService;
    private final OAuthService oAuthService;
    private final TurnstileService turnstileService;
    private final StringRedisTemplate redisTemplate;

private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.ok(captchaService.getCaptcha());
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    @Audit(action = "USER_REGISTER", module = "USER", targetType = "user", targetIdResult = "id")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        turnstileService.verify(dto.getTurnstileToken(), clientIp(request));
        return Result.ok(userService.register(dto));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @Audit(action = "USER_LOGIN", module = "USER", targetType = "user")
    public Result<String> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        turnstileService.verify(dto.getTurnstileToken(), clientIp(request));
        return Result.ok(userService.login(dto));
    }

    @Operation(summary = "用户退出")
    @PostMapping("/logout")
    @Audit(action = "USER_LOGOUT", module = "USER", targetType = "user")
    public Result<Void> logout() {
        userService.logout();
        return Result.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(userService.getUserVOById(StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "获取用户公开信息")
    @GetMapping("/profile/{uuid}")
    public Result<UserProfileVO> profile(@PathVariable String uuid) {
        UserProfileVO profile = userService.getUserProfileByUuid(uuid);
        boolean canViewContact = StpUtil.isLogin()
                && (Objects.equals(profile.getId(), StpUtil.getLoginIdAsLong()) || StpUtil.hasRole("admin"));
        if (!canViewContact) {
            profile.setEmail(null);
            profile.setPhone(null);
        }
        return Result.ok(profile);
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    @Audit(action = "USER_PROFILE_UPDATE", module = "USER", targetType = "user")
    public Result<UserVO> updateProfile(@RequestBody Map<String, String> body) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.updateProfile(userId,
                body.get("displayName"), body.get("email"),
                body.get("phone"), body.get("bio"), body.get("emailCode")));
    }

    @Operation(summary = "发送邮箱变更验证码")
    @PostMapping("/email-change-code")
    @Audit(action = "USER_EMAIL_CHANGE_CODE", module = "USER", targetType = "user")
    public Result<Void> sendEmailChangeCode(@RequestBody Map<String, String> body) {
        userService.sendEmailChangeCode(StpUtil.getLoginIdAsLong(), body.get("email"));
        return Result.ok();
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    @Audit(action = "USER_AVATAR_UPLOAD", module = "USER", targetType = "user")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        long userId = StpUtil.getLoginIdAsLong();
        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("仅支持图片格式");
        String objectKey = "avatars/" + UUID.randomUUID() + "." + ext;
        String mimeType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
        storageService.upload("avatars", objectKey, file.getBytes(), mimeType, ImageUrlService.PUBLIC_CACHE_CONTROL);
        userService.updateAvatar(userId, objectKey);
        return Result.ok(userService.getUserVOById(userId).getAvatarUrl());
    }

    @Operation(summary = "上传背景")
    @PostMapping("/background")
    @Audit(action = "USER_BACKGROUND_UPLOAD", module = "USER", targetType = "user")
    public Result<String> uploadBackground(@RequestParam("file") MultipartFile file) throws IOException {
        long userId = StpUtil.getLoginIdAsLong();
        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("仅支持图片格式");
        String objectKey = "backgrounds/" + UUID.randomUUID() + "." + ext;
        String mimeType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
        storageService.upload("backgrounds", objectKey, file.getBytes(), mimeType, ImageUrlService.PUBLIC_CACHE_CONTROL);
        userService.updateBackground(userId, objectKey);
        return Result.ok(userService.getUserVOById(userId).getBackgroundUrl());
    }

    @Operation(summary = "下载头像")
    @GetMapping("/avatar/{uuid}")
    public ResponseEntity<byte[]> avatar(@PathVariable String uuid) {
        User user = getExistingUserByUuid(uuid);
        return userMedia("avatars", user.getAvatarKey(), user.getAvatar());
    }

    @Operation(summary = "下载背景")
    @GetMapping("/background/{uuid}")
    public ResponseEntity<byte[]> background(@PathVariable String uuid) {
        User user = getExistingUserByUuid(uuid);
        return userMedia("backgrounds", user.getBackgroundKey(), user.getBackground());
    }

    @Operation(summary = "管理员获取用户列表")
    @GetMapping("/list")
    @SaCheckPermission("user:manage")
    public Result<Page<UserVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(userService.getUserList(page, limit));
    }

    @Operation(summary = "GitHub OAuth授权地址")
    @GetMapping("/oauth/github")
    public Result<Map<String, String>> githubOAuth(HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        String url = oAuthService.getAuthorizeUrl("github", baseUrl);
        return Result.ok(Map.of("authorizeUrl", url));
    }

    @Operation(summary = "GitHub OAuth回调")
    @GetMapping("/oauth/github/callback")
    public void githubCallback(@RequestParam String code, @RequestParam String state,
                                jakarta.servlet.http.HttpServletResponse response,
                                HttpServletRequest request) throws java.io.IOException {
        String baseUrl = buildBaseUrl(request);
        OAuthService.OAuthResult result = oAuthService.handleCallback("github", code, state, baseUrl);
        response.sendRedirect(buildLoginRedirect(result.token(), result.baseUrl()));
    }

    @Operation(summary = "Google OAuth授权地址")
    @GetMapping("/oauth/google")
    public Result<Map<String, String>> googleOAuth(HttpServletRequest request) {
        String baseUrl = buildBaseUrl(request);
        String url = oAuthService.getAuthorizeUrl("google", baseUrl);
        return Result.ok(Map.of("authorizeUrl", url));
    }

    @Operation(summary = "Google OAuth回调")
    @GetMapping("/oauth/google/callback")
    public void googleCallback(@RequestParam String code, @RequestParam String state,
                                jakarta.servlet.http.HttpServletResponse response,
                                HttpServletRequest request) throws java.io.IOException {
        String baseUrl = buildBaseUrl(request);
        OAuthService.OAuthResult result = oAuthService.handleCallback("google", code, state, baseUrl);
        response.sendRedirect(buildLoginRedirect(result.token(), result.baseUrl()));
    }

    @Operation(summary = "检查邮箱/手机号是否已被使用")
    @GetMapping("/check-field")
    public Result<Void> checkField(@RequestParam String field,
                                    @RequestParam String value,
                                    @RequestParam(required = false) Long excludeId) {
        if (!"email".equals(field) && !"phone".equals(field)) {
            return Result.error(400, "参数错误");
        }
        userService.checkField(field, value, excludeId);
        return Result.ok();
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-code")
    @Audit(action = "USER_SEND_CODE", module = "USER", targetType = "user")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto, HttpServletRequest request) {
        turnstileService.verify(dto.getTurnstileToken(), clientIp(request));
        userService.sendCode(dto.getEmail().trim(), dto.getCaptchaId(), dto.getCaptchaCode(), dto.getPurpose());
        return Result.ok();
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/login-by-code")
    @Audit(action = "USER_CODE_LOGIN", module = "USER", targetType = "user")
    public Result<String> loginByCode(@Valid @RequestBody CodeLoginDTO dto, HttpServletRequest request) {
        turnstileService.verify(dto.getTurnstileToken(), clientIp(request));
        return Result.ok(userService.loginByCode(dto));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    @Audit(action = "USER_PASSWORD_CHANGE", module = "USER", targetType = "user")
    public Result<Void> changePassword(@Valid @RequestBody com.picmgmt.dto.ChangePasswordDTO dto) {
        userService.changePassword(StpUtil.getLoginIdAsLong(),
                dto.getOldPassword(), dto.getNewPassword());
        return Result.ok();
    }

    @Operation(summary = "注销账号")
    @DeleteMapping("/account")
    @Audit(action = "USER_ACCOUNT_DELETE", module = "USER", targetType = "user")
    public Result<Void> deleteAccount() {
        userService.deleteAccount(StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    private User getExistingUser(Long id) {
        User user = userService.getById(id);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private User getExistingUserByUuid(String uuid) {
        User user = userService.getByUuid(uuid);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private static final Set<String> TRUSTED_PROXY_PREFIXES = Set.of(
            "127.0.0.1", "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
            "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.",
            "192.168.", "::1"
    );

    private String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean fromTrustedProxy = TRUSTED_PROXY_PREFIXES.stream().anyMatch(remoteAddr::startsWith);
        if (fromTrustedProxy) {
            String cfIp = request.getHeader("CF-Connecting-IP");
            if (cfIp != null && !cfIp.isBlank()) {
                return cfIp.trim();
            }
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return remoteAddr;
    }

    private static final Set<String> ALLOWED_OAUTH_HOSTS = Set.of(
        "image-space.app", "admin.image-space.app"
    );

    private String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = "https";
        }
        String host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            return "https://image-space.app";
        }
        String hostname = host.contains(":") ? host.substring(0, host.indexOf(':')) : host;
        if (!ALLOWED_OAUTH_HOSTS.contains(hostname)) {
            log.warn("Blocked OAuth request with unexpected Host header: {}", host);
            return "https://image-space.app";
        }
        return scheme + "://" + host;
    }

    private String buildLoginRedirect(String token, String baseUrl) {
        String url = baseUrl;
        if (url == null || url.isBlank()) {
            url = "https://image-space.app";
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("oauth:code:" + code, token, Duration.ofSeconds(60));
        return url + "/login?oauth_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
    }

    @Operation(summary = "OAuth一次性code换取token")
    @PostMapping("/oauth/exchange")
    @Audit(action = "USER_OAUTH_EXCHANGE", module = "USER", targetType = "user")
    public Result<Map<String, String>> exchangeOAuthCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return Result.error(400, "缺少code参数");
        }
        String token = redisTemplate.opsForValue().get("oauth:code:" + code);
        if (token == null || token.isBlank()) {
            return Result.error(400, "code已过期或无效");
        }
        redisTemplate.delete("oauth:code:" + code);
        return Result.ok(Map.of("satoken", token));
    }

    private ResponseEntity<byte[]> userMedia(String bucket, String storageKey, String legacyValue) {
        LegacyDataUri legacyData = LegacyDataUri.parse(legacyValue).orElse(null);
        if (legacyData == null && legacyValue != null
                && legacyValue.regionMatches(true, 0, "data:", 0, 5)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String objectValue = storageKey != null && !storageKey.isBlank()
                ? storageKey
                : legacyData == null ? legacyValue : null;

        if (objectValue != null && !objectValue.isBlank()) {
            String objectKey = resolveObjectKey(bucket, objectValue);
            try {
                MediaType contentType = mediaType(objectKey);
                if (MediaType.APPLICATION_OCTET_STREAM.equals(contentType) && legacyData != null) {
                    contentType = MediaType.parseMediaType(legacyData.contentType());
                }
                return mediaResponse(contentType, storageService.download(bucket, objectKey));
            } catch (BusinessException e) {
                if (legacyData == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, e);
                }
                log.warn("Object storage media unavailable for bucket={}, using legacy data", bucket);
            }
        }

        if (legacyData != null) {
            return mediaResponse(MediaType.parseMediaType(legacyData.contentType()), legacyData.bytes());
        }
        throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    private ResponseEntity<byte[]> mediaResponse(MediaType contentType, byte[] bytes) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(contentType)
                .body(bytes);
    }

    private String resolveObjectKey(String bucket, String value) {
        String key = value;
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            try {
                key = URI.create(key).getPath();
            } catch (IllegalArgumentException ignored) {
                throw new BusinessException(ErrorCode.NOT_FOUND);
            }
        }
        String storagePrefix = "/storage/" + bucket + "/";
        String bucketPrefix = "/" + bucket + "/";
        if (key.startsWith(storagePrefix)) {
            key = key.substring(storagePrefix.length());
        } else if (key.startsWith(bucketPrefix)) {
            key = key.substring(bucketPrefix.length());
        } else if (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (key.isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return key;
    }

    private MediaType mediaType(String path) {
        String ext = FileUtil.extName(path).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

}
