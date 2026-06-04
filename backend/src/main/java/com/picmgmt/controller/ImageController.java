package com.picmgmt.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.annotation.Audit;
import com.picmgmt.common.Result;
import com.picmgmt.dto.ImageQueryDTO;
import com.picmgmt.image.ImageReadService;
import com.picmgmt.image.ImageUpdateDTO;
import com.picmgmt.image.ImageUrlService;
import com.picmgmt.image.ImageWriteService;
import com.picmgmt.like.LikeTarget;
import com.picmgmt.service.LikeService;
import com.picmgmt.vo.ImageVO;
import com.picmgmt.vo.LikeStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Tag(name = "图片模块")
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageWriteService imageWriteService;
    private final ImageReadService imageReadService;
    private final LikeService likeService;
    private final ImageUrlService imageUrlService;

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    @SaCheckPermission("image:upload")
    @Audit(action = "IMAGE_UPLOAD", module = "IMAGE", targetType = "image")
    public Result<ImageVO> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) String imageName,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) String tags,
                                   @RequestParam(required = false) String visibility,
                                   @RequestParam(required = false) String visibleUsernames) {
        return Result.ok(imageWriteService.upload(file, categoryId, description, tags, visibility, visibleUsernames, imageName));
    }

    @Operation(summary = "删除图片")
    @DeleteMapping("/{uuid}")
    @SaCheckPermission("image:delete")
    @Audit(action = "IMAGE_DELETE", module = "IMAGE", targetType = "image")
    public Result<Void> delete(@PathVariable String uuid) {
        imageWriteService.deleteByUuid(uuid);
        return Result.ok();
    }

    @Operation(summary = "更新图片信息")
    @PutMapping("/{uuid}")
    @SaCheckPermission("image:edit")
    @Audit(action = "IMAGE_UPDATE", module = "IMAGE", targetType = "image")
    public Result<ImageVO> update(@PathVariable String uuid, @RequestBody @Valid ImageUpdateDTO dto) {
        return Result.ok(imageWriteService.updateByUuid(uuid, dto));
    }

    @Operation(summary = "查询当前用户图片列表")
    @GetMapping("/list")
    public Result<Page<ImageVO>> list(@Valid ImageQueryDTO dto) {
        return Result.ok(imageReadService.page(dto));
    }

    @Operation(summary = "查询用户公开图片列表")
    @GetMapping("/user/{userUuid}")
    public Result<Page<ImageVO>> userPublicImages(@PathVariable String userUuid,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "50") Integer limit,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String tags,
                                                  @RequestParam(defaultValue = "latest") String sortMode,
                                                  @RequestParam(required = false) String randomSeed,
                                                  @RequestParam(required = false) String sortField,
                                                  @RequestParam(required = false) String sortOrder) {
        return Result.ok(imageReadService.getPublicByUserUuid(userUuid, page, limit, keyword, tags, sortMode, randomSeed, sortField, sortOrder));
    }

    @Operation(summary = "下载图片")
    @GetMapping("/download/{uuid}")
    public ResponseEntity<byte[]> download(@PathVariable String uuid) {
        var vo = imageReadService.getByUuid(uuid);
        byte[] bytes = imageReadService.downloadByUuid(uuid);
        String encodedName = URLEncoder.encode(vo.getImageName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        CacheControl cacheControl = "PUBLIC".equals(vo.getVisibility())
                ? CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic()
                : CacheControl.noStore();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(mediaType(vo.getImageType()))
                .cacheControl(cacheControl)
                .body(bytes);
    }

    @Operation(summary = "Worker 私有媒体鉴权")
    @GetMapping("/media/authorize")
    public ResponseEntity<Void> authorizeMedia(@RequestParam("key") String storageKey,
                                               @RequestParam("token") String token) {
        boolean allowed = imageUrlService.authorizePrivateAccess(storageKey, token);
        CacheControl cacheControl = CacheControl.noStore();
        if (!allowed) {
            return ResponseEntity.status(403).cacheControl(cacheControl).build();
        }
        return ResponseEntity.noContent().cacheControl(cacheControl).build();
    }

    @Operation(summary = "获取图片详情")
    @GetMapping("/{uuid}")
    public Result<ImageVO> getById(@PathVariable String uuid) {
        return Result.ok(imageReadService.getByUuid(uuid));
    }

    @Operation(summary = "图片广场")
    @GetMapping("/square")
    public Result<Page<ImageVO>> square(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "50") Integer limit,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String tags,
                                        @RequestParam(defaultValue = "random") String sortMode,
                                        @RequestParam(required = false) String randomSeed,
                                        @RequestParam(required = false) String sortField,
                                        @RequestParam(required = false) String sortOrder) {
        return Result.ok(imageReadService.getSquare(page, limit, keyword, tags, sortMode, randomSeed, sortField, sortOrder));
    }

    @Operation(summary = "点赞图片")
    @PostMapping("/{uuid}/like")
    @Audit(action = "IMAGE_LIKE", module = "IMAGE", targetType = "image")
    public Result<LikeStatusVO> like(@PathVariable String uuid) {
        Long id = imageReadService.resolveImageId(uuid);
        return Result.ok(likeService.like(LikeTarget.IMAGE, id));
    }

    @Operation(summary = "取消点赞图片")
    @DeleteMapping("/{uuid}/like")
    @Audit(action = "IMAGE_UNLIKE", module = "IMAGE", targetType = "image")
    public Result<LikeStatusVO> unlike(@PathVariable String uuid) {
        Long id = imageReadService.resolveImageId(uuid);
        return Result.ok(likeService.unlike(LikeTarget.IMAGE, id));
    }

    private MediaType mediaType(String imageType) {
        String type = imageType == null ? "" : imageType.trim().toLowerCase();
        return switch (type) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
