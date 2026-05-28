package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Comment;
import com.picmgmt.entity.CommentLike;
import com.picmgmt.mapper.CommentLikeMapper;
import com.picmgmt.service.CommentLikeService;
import com.picmgmt.service.CommentService;
import com.picmgmt.service.NotificationService;
import com.picmgmt.vo.CommentLikeStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentLikeMapper commentLikeMapper;
    private final CommentService commentService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CommentLikeStatusVO like(Long commentId) {
        long userId = StpUtil.getLoginIdAsLong();
        Comment comment = commentService.getById(commentId);
        CommentLike existing = commentLikeMapper.findByCommentIdAndUserId(commentId, userId);
        if (existing == null) {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            commentLikeMapper.insert(like);
            if (!comment.getUserId().equals(userId)) {
                notificationService.createCommentLikeNotification(comment, userId);
            }
        }
        return status(commentId, true);
    }

    @Override
    @Transactional
    public CommentLikeStatusVO unlike(Long commentId) {
        long userId = StpUtil.getLoginIdAsLong();
        commentService.getById(commentId);
        commentLikeMapper.deleteByCommentIdAndUserId(commentId, userId);
        return status(commentId, false);
    }

    private CommentLikeStatusVO status(Long commentId, boolean likedByMe) {
        Long count = commentLikeMapper.countByCommentId(commentId);
        return new CommentLikeStatusVO(commentId, count == null ? 0L : count, likedByMe);
    }
}
