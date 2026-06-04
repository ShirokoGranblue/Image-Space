package com.picmgmt.controller;

import cn.hutool.core.io.FileUtil;
import com.picmgmt.annotation.Audit;
import com.picmgmt.common.Result;
import com.picmgmt.entity.Comment;
import com.picmgmt.service.CommentService;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final StorageService storageService;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @Operation(summary = "上传评论图片")
    @PostMapping("/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("文件不能为空");
        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) throw new IllegalArgumentException("仅支持 JPG/PNG/WEBP 格式");

        String objectKey = "comments/" + UUID.randomUUID() + "." + ext;
        String mimeType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
        storageService.upload("comments", objectKey, file.getBytes(), mimeType);
        return Result.ok(objectKey);
    }

    @Operation(summary = "下载评论图片")
    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        Comment comment = commentService.getById(id);
        byte[] bytes = storageService.download("comments", comment.getImagePath());
        return ResponseEntity.ok()
            .contentType(getMediaType(comment.getImagePath()))
            .body(bytes);
    }

    private MediaType getMediaType(String path) {
        String ext = FileUtil.extName(path).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    @Operation(summary = "添加评论")
    @PostMapping
    @Audit(action = "COMMENT_ADD", module = "COMMENT", targetType = "comment")
    public Result<Comment> add(@RequestBody Map<String, String> body) {
        return Result.ok(commentService.add(
                Long.valueOf(body.get("imageId")), body.get("content"), body.get("imagePath")));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    @Audit(action = "COMMENT_DELETE", module = "COMMENT", targetType = "comment")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "获取图片评论列表")
    @GetMapping("/list/{imageUuid}")
    public Result<List<CommentVO>> list(@PathVariable String imageUuid) {
        return Result.ok(commentService.listByImageUuid(imageUuid));
    }
}
