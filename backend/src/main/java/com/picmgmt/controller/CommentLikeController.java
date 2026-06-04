package com.picmgmt.controller;

import com.picmgmt.annotation.Audit;
import com.picmgmt.common.Result;
import com.picmgmt.like.LikeTarget;
import com.picmgmt.service.LikeService;
import com.picmgmt.vo.LikeStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "评论点赞模块")
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentLikeController {

    private final LikeService likeService;

    @Operation(summary = "点赞评论")
    @PostMapping("/{id}/like")
    @Audit(action = "COMMENT_LIKE", module = "COMMENT", targetType = "comment")
    public Result<LikeStatusVO> like(@PathVariable Long id) {
        return Result.ok(likeService.like(LikeTarget.COMMENT, id));
    }

    @Operation(summary = "取消点赞评论")
    @DeleteMapping("/{id}/like")
    @Audit(action = "COMMENT_UNLIKE", module = "COMMENT", targetType = "comment")
    public Result<LikeStatusVO> unlike(@PathVariable Long id) {
        return Result.ok(likeService.unlike(LikeTarget.COMMENT, id));
    }
}
