package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Comment;
import com.picmgmt.entity.CommentLike;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.ImageLike;
import com.picmgmt.like.LikeTarget;
import com.picmgmt.image.ImagePermissionService;
import com.picmgmt.mapper.CommentLikeMapper;
import com.picmgmt.mapper.ImageLikeMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.service.CommentService;
import com.picmgmt.service.LikeService;
import com.picmgmt.service.NotificationService;
import com.picmgmt.vo.LikeStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final ImageLikeMapper imageLikeMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final ImageRepository imageRepository;
    private final CommentService commentService;
    private final NotificationService notificationService;
    private final ImagePermissionService imagePermissionService;

    @Override
    @Transactional
    public LikeStatusVO like(LikeTarget target, Long targetId) {
        long userId = StpUtil.getLoginIdAsLong();
        if (target == LikeTarget.IMAGE) {
            Image image = requireVisibleImage(targetId);
            ImageLike existing = imageLikeMapper.findByImageIdAndUserId(targetId, userId);
            if (existing == null) {
                ImageLike like = new ImageLike();
                like.setImageId(targetId);
                like.setUserId(userId);
                like.setCreateTime(LocalDateTime.now());
                imageLikeMapper.insert(like);
                if (!image.getUserId().equals(userId)) {
                    notificationService.createLikeNotification(image, userId);
                }
            }
            return status(target, targetId, true);
        } else {
            Comment comment = commentService.getById(targetId);
            requireVisibleImage(comment.getImageId());
            CommentLike existing = commentLikeMapper.findByCommentIdAndUserId(targetId, userId);
            if (existing == null) {
                CommentLike like = new CommentLike();
                like.setCommentId(targetId);
                like.setUserId(userId);
                like.setCreateTime(LocalDateTime.now());
                commentLikeMapper.insert(like);
                if (!comment.getUserId().equals(userId)) {
                    notificationService.createCommentLikeNotification(comment, userId);
                }
            }
            return status(target, targetId, true);
        }
    }

    @Override
    @Transactional
    public LikeStatusVO unlike(LikeTarget target, Long targetId) {
        long userId = StpUtil.getLoginIdAsLong();
        if (target == LikeTarget.IMAGE) {
            requireVisibleImage(targetId);
            imageLikeMapper.deleteByImageIdAndUserId(targetId, userId);
        } else {
            Comment comment = commentService.getById(targetId);
            requireVisibleImage(comment.getImageId());
            commentLikeMapper.deleteByCommentIdAndUserId(targetId, userId);
        }
        return status(target, targetId, false);
    }

    @Override
    @Transactional
    public void deleteAllByTarget(LikeTarget target, Long targetId) {
        if (target == LikeTarget.IMAGE) {
            imageLikeMapper.deleteByImageId(targetId);
        } else {
            commentLikeMapper.deleteByCommentId(targetId);
        }
    }

    private LikeStatusVO status(LikeTarget target, Long targetId, boolean likedByMe) {
        Long count = target == LikeTarget.IMAGE
                ? imageLikeMapper.countByImageId(targetId)
                : commentLikeMapper.countByCommentId(targetId);
        return new LikeStatusVO(targetId, count == null ? 0L : count, likedByMe);
    }

    private Image requireVisibleImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!imagePermissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        return image;
    }
}
