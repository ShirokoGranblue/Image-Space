package com.picmgmt.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.Result;
import com.picmgmt.dto.LoginDTO;
import com.picmgmt.dto.RegisterDTO;
import com.picmgmt.service.ImageCacheService;
import com.picmgmt.service.UserService;
import com.picmgmt.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ImageCacheService imageCacheService;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

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
        return Result.ok(userService.updateProfile(
                userId,
                body.get("displayName"),
                body.get("email"),
                body.get("phone"),
                body.get("bio")));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        String dataUrl = convertToDataUrl(file);
        long userId = StpUtil.getLoginIdAsLong();
        // 写入缓存
        imageCacheService.encodeToDataUrl("avatar:" + userId, file.getBytes(),
                ImageCacheService.getMimeType(FileUtil.extName(file.getOriginalFilename()).toLowerCase()));
        userService.updateAvatar(userId, dataUrl);
        return Result.ok(dataUrl);
    }

    @Operation(summary = "上传背景")
    @PostMapping("/background")
    public Result<String> uploadBackground(@RequestParam("file") MultipartFile file) throws IOException {
        String dataUrl = convertToDataUrl(file);
        long userId = StpUtil.getLoginIdAsLong();
        // 写入缓存
        imageCacheService.encodeToDataUrl("background:" + userId, file.getBytes(),
                ImageCacheService.getMimeType(FileUtil.extName(file.getOriginalFilename()).toLowerCase()));
        userService.updateBackground(userId, dataUrl);
        return Result.ok(dataUrl);
    }

    @SaCheckRole("admin")
    @Operation(summary = "管理员获取用户列表")
    @GetMapping("/list")
    public Result<Page<UserVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(userService.getUserList(page, limit));
    }

    /**
     * 将 MultipartFile 转换为 Base64 Data URL
     */
    private String convertToDataUrl(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("文件不能为空");
        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext))
            throw new IllegalArgumentException("仅支持图片格式");

        byte[] bytes = file.getBytes();
        String mimeType = ImageCacheService.getMimeType(ext);
        return "data:" + mimeType + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
    }
}
