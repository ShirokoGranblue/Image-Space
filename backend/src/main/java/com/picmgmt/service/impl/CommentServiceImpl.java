package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Comment;
import com.picmgmt.image.ImagePermissionService;
import com.picmgmt.mapper.CommentLikeMapper;
import com.picmgmt.repository.CommentRepository;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.service.CommentService;
import com.picmgmt.service.NotificationService;
import com.picmgmt.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ImageRepository imageRepository;
    private final NotificationService notificationService;
    private final CommentLikeMapper commentLikeMapper;
    private final ImagePermissionService imagePermissionService;

    @Override
    public Comment add(Long imageId, String content, String imagePath) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.COMMENT_EMPTY);
        }
        var image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!imagePermissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }

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
        List<CommentVO> list = commentRepository.listByImageId(imageId);
        decorateLikeInfo(list);
        return list;
    }

    @Override
    public List<CommentVO> listByImageUuid(String imageUuid) {
        var image = imageRepository.findByUuid(imageUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));
        if (!imagePermissionService.canView(image)) {
            throw new BusinessException(ErrorCode.IMAGE_PERMISSION_DENIED);
        }
        return listByImage(image.getId());
    }

    private void decorateLikeInfo(List<CommentVO> list) {
        if (list == null || list.isEmpty()) return;

        List<Long> commentIds = list.stream().map(CommentVO::getId).filter(id -> id != null).toList();
        if (commentIds.isEmpty()) return;

        // Batch fetch like counts: 1 query
        Map<Long, Long> likeCounts;
        try {
            List<Map<String, Object>> counts = commentLikeMapper.countByCommentIds(commentIds);
            likeCounts = counts.stream().collect(Collectors.toMap(
                    m -> ((Number) m.get("comment_id")).longValue(),
                    m -> ((Number) m.get("cnt")).longValue(),
                    (a, b) -> a
            ));
        } catch (Exception e) {
            likeCounts = Collections.emptyMap();
        }

        // Batch fetch user-liked status: 1 query (only if logged in)
        Set<Long> likedByMeIds;
        if (StpUtil.isLogin()) {
            long userId = StpUtil.getLoginIdAsLong();
            try {
                List<Long> likedIds = commentLikeMapper.findLikedCommentIdsByUser(commentIds, userId);
                likedByMeIds = Set.copyOf(likedIds);
            } catch (Exception e) {
                likedByMeIds = Collections.emptySet();
            }
        } else {
            likedByMeIds = Collections.emptySet();
        }

        for (CommentVO vo : list) {
            if (vo.getId() != null) {
                vo.setLikeCount(likeCounts.getOrDefault(vo.getId(), 0L));
                vo.setLikedByMe(likedByMeIds.contains(vo.getId()));
            }
        }
    }
}
