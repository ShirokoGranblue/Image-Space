package com.picmgmt.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.common.Result;
import com.picmgmt.dto.CodeLoginDTO;
import com.picmgmt.dto.LoginDTO;
import com.picmgmt.dto.RegisterDTO;
import com.picmgmt.dto.SendCodeDTO;
import com.picmgmt.entity.User;
import com.picmgmt.service.CaptchaService;
import com.picmgmt.service.OAuthService;
import com.picmgmt.service.UserService;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StorageService storageService;
    private final CaptchaService captchaService;
    private final OAuthService oAuthService;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.ok(captchaService.getCaptcha());
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @Operation(summary = "用户退出")
    @PostMapping("/logout")
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
    @GetMapping("/profile/{id}")
    public Result<UserVO> profile(@PathVariable Long id) {
        return Result.ok(userService.getUserVOById(id));
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody Map<String, String> body) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userService.updateProfile(userId,
                body.get("displayName"), body.get("email"),
                body.get("phone"), body.get("bio")));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        long userId = StpUtil.getLoginIdAsLong();
        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("仅支持图片格式");
        String objectKey = userId + "/" + UUID.randomUUID() + "." + ext;
        String mimeType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
        storageService.upload("avatars", objectKey, file.getBytes(), mimeType);
        userService.updateAvatar(userId, objectKey);
        return Result.ok(userMediaUrl("avatar", userId));
    }

    @Operation(summary = "上传背景")
    @PostMapping("/background")
    public Result<String> uploadBackground(@RequestParam("file") MultipartFile file) throws IOException {
        long userId = StpUtil.getLoginIdAsLong();
        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("仅支持图片格式");
        String objectKey = userId + "/" + UUID.randomUUID() + "." + ext;
        String mimeType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
        storageService.upload("backgrounds", objectKey, file.getBytes(), mimeType);
        userService.updateBackground(userId, objectKey);
        return Result.ok(userMediaUrl("background", userId));
    }

    @Operation(summary = "下载头像")
    @GetMapping("/avatar/{id}")
    public ResponseEntity<byte[]> avatar(@PathVariable Long id) {
        User user = getExistingUser(id);
        String objectKey = resolveObjectKey("avatars", user.getAvatarKey(), user.getAvatar());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(mediaType(objectKey))
                .body(storageService.download("avatars", objectKey));
    }

    @Operation(summary = "下载背景")
    @GetMapping("/background/{id}")
    public ResponseEntity<byte[]> background(@PathVariable Long id) {
        User user = getExistingUser(id);
        String objectKey = resolveObjectKey("backgrounds", user.getBackgroundKey(), user.getBackground());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(mediaType(objectKey))
                .body(storageService.download("backgrounds", objectKey));
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
    public Result<Map<String, String>> githubOAuth() {
        String url = oAuthService.getAuthorizeUrl("github");
        return Result.ok(Map.of("authorizeUrl", url));
    }

    @Operation(summary = "GitHub OAuth回调")
    @GetMapping("/oauth/github/callback")
    public void githubCallback(@RequestParam String code, @RequestParam String state,
                                jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String token = oAuthService.handleCallback("github", code, state);
        response.sendRedirect("http://localhost:3000/login?satoken=" + token);
    }

    @Operation(summary = "Google OAuth授权地址")
    @GetMapping("/oauth/google")
    public Result<Map<String, String>> googleOAuth() {
        String url = oAuthService.getAuthorizeUrl("google");
        return Result.ok(Map.of("authorizeUrl", url));
    }

    @Operation(summary = "Google OAuth回调")
    @GetMapping("/oauth/google/callback")
    public void googleCallback(@RequestParam String code, @RequestParam String state,
                                jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String token = oAuthService.handleCallback("google", code, state);
        response.sendRedirect("http://localhost:3000/login?satoken=" + token);
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
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        userService.sendCode(dto.getEmail().trim(), dto.getCaptchaId(), dto.getCaptchaCode());
        return Result.ok();
    }

    @Operation(summary = "邮箱验证码登录")
    @PostMapping("/login-by-code")
    public Result<String> loginByCode(@Valid @RequestBody CodeLoginDTO dto) {
        return Result.ok(userService.loginByCode(dto));
    }

    @Operation(summary = "注销账号")
    @DeleteMapping("/account")
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

    private String userMediaUrl(String type, Long userId) {
        return "/api/user/" + type + "/" + userId;
    }

    private String resolveObjectKey(String bucket, String storageKey, String legacyValue) {
        String key = storageKey != null && !storageKey.isBlank() ? storageKey : legacyValue;
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
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

}
