package com.picmgmt.controller;

import com.picmgmt.common.Result;
import com.picmgmt.service.CommentLikeService;
import com.picmgmt.vo.CommentLikeStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "评论点赞模块")
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @Operation(summary = "点赞评论")
    @PostMapping("/{id}/like")
    public Result<CommentLikeStatusVO> like(@PathVariable Long id) {
        return Result.ok(commentLikeService.like(id));
    }

    @Operation(summary = "取消点赞评论")
    @DeleteMapping("/{id}/like")
    public Result<CommentLikeStatusVO> unlike(@PathVariable Long id) {
        return Result.ok(commentLikeService.unlike(id));
    }
}
