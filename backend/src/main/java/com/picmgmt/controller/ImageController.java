package com.picmgmt.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.Result;
import com.picmgmt.dto.ImageQueryDTO;
import com.picmgmt.service.ImageService;
import com.picmgmt.vo.ImageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "图片模块")
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Value("${app.upload-path:./upload}")
    private String uploadPath;

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public Result<ImageVO> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) String tags,
                                   @RequestParam(required = false) String visibility,
                                   @RequestParam(required = false) String visibleUsernames) {
        return Result.ok(imageService.upload(file, categoryId, description, tags, visibility, visibleUsernames));
    }

    @Operation(summary = "删除图片")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "更新图片信息")
    @PutMapping("/{id}")
    public Result<ImageVO> update(@PathVariable Long id,
                                   @RequestParam(required = false) String imageName,
                                   @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) String tags,
                                   @RequestParam(required = false) String visibility,
                                   @RequestParam(required = false) String visibleUsernames) {
        return Result.ok(imageService.update(id, imageName, categoryId, description, tags, visibility, visibleUsernames));
    }

    @Operation(summary = "查询当前用户图片列表")
    @GetMapping("/list")
    public Result<Page<ImageVO>> list(@ModelAttribute ImageQueryDTO dto) {
        return Result.ok(imageService.page(dto));
    }

    @Operation(summary = "下载图片")
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ImageVO vo = imageService.getById(id);
        File file = new File(uploadPath, vo.getImagePath().replace("/upload", ""));
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = FileUtil.readBytes(file);
        String encodedName = URLEncoder.encode(vo.getImageName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @Operation(summary = "获取图片详情")
    @GetMapping("/{id}")
    public Result<ImageVO> getById(@PathVariable Long id) {
        return Result.ok(imageService.getById(id));
    }

    @Operation(summary = "图片广场")
    @GetMapping("/square")
    public Result<Page<ImageVO>> square(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "12") Integer limit) {
        return Result.ok(imageService.getSquare(page, limit));
    }
}
