package com.picmgmt.controller;

import cn.hutool.core.io.FileUtil;
import com.picmgmt.common.Result;
import com.picmgmt.entity.Comment;
import com.picmgmt.service.CommentService;
import com.picmgmt.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Tag(name = "评论模块")
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Value("${app.upload-path:./upload}")
    private String uploadPath;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    @Operation(summary = "上传评论图片")
    @PostMapping("/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("文件不能为空");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) throw new IllegalArgumentException("文件名无效");

        String ext = FileUtil.extName(originalFilename).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("仅支持 JPG、PNG、WEBP 格式");

        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        LocalDate now = LocalDate.now();
        String relativeDir = "comments/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue());
        String fileName = UUID.randomUUID() + "." + ext;

        File dir = basePath.resolve(relativeDir).toFile();
        if (!dir.exists()) dir.mkdirs();

        File dest = new File(dir, fileName);
        file.transferTo(dest);
        return Result.ok("/upload/" + relativeDir + "/" + fileName);
    }

    @Operation(summary = "添加评论")
    @PostMapping
    public Result<Comment> add(@RequestBody Map<String, String> body) {
        return Result.ok(commentService.add(
                Long.valueOf(body.get("imageId")), body.get("content"), body.get("imagePath")));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "获取图片评论列表")
    @GetMapping("/list/{imageId}")
    public Result<List<CommentVO>> list(@PathVariable Long imageId) {
        return Result.ok(commentService.listByImage(imageId));
    }
}
