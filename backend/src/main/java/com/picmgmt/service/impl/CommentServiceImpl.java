package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Comment;
import com.picmgmt.repository.CommentRepository;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.service.CommentService;
import com.picmgmt.service.NotificationService;
import com.picmgmt.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ImageRepository imageRepository;
    private final NotificationService notificationService;

    @Override
    public Comment add(Long imageId, String content, String imagePath) {
        if ((content == null || content.isBlank()) && (imagePath == null || imagePath.isBlank())) {
            throw new BusinessException(ErrorCode.COMMENT_EMPTY);
        }
        var image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        long actorUserId = StpUtil.getLoginIdAsLong();
        Comment comment = new Comment();
        comment.setImageId(imageId);
        comment.setUserId(actorUserId);
        comment.setContent(content);
        // 如果是完整 URL (如 http://host:port/comments/uuid.png?...)，提取纯 storage key
        String key = imagePath;
        if (imagePath != null && imagePath.startsWith("http")) {
            String path = imagePath.substring(imagePath.indexOf("/", imagePath.indexOf("://") + 3)); // /bucket/key?...
            int queryIdx = path.indexOf("?");
            if (queryIdx > 0) path = path.substring(0, queryIdx);
            int lastSlash = path.lastIndexOf("/");
            key = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        }
        comment.setImagePath(key);
        commentRepository.insert(comment);
        if (image.getUserId() != null && !image.getUserId().equals(actorUserId)) {
            notificationService.createCommentNotification(image, actorUserId, comment.getId(), content);
        }
        return comment;
    }

    @Override
    public Comment getById(Long commentId) {
        Comment comment = commentRepository.findById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    @Override
    public void delete(Long commentId) {
        Comment comment = commentRepository.findById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        long userId = StpUtil.getLoginIdAsLong();
        if (!StpUtil.hasRole("admin") && !comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentVO> listByImage(Long imageId) {
        return commentRepository.listByImageId(imageId);
    }
}
