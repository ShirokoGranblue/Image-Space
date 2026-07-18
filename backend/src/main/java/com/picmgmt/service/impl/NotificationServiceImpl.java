package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Comment;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.Notification;
import com.picmgmt.entity.User;
import com.picmgmt.image.ImageUrlService;
import com.picmgmt.mapper.NotificationMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.service.NotificationService;
import com.picmgmt.storage.StorageService;
import com.picmgmt.util.MediaUrlUtil;
import com.picmgmt.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int PREVIEW_LIMIT = 80;
    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Shanghai");

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final StorageService storageService;
    private final MediaUrlUtil mediaUrlUtil;
    private final ImageUrlService imageUrlService;
    private final Clock clock;

    @Override
    @Transactional
    public void createCommentNotification(Image image, Long actorUserId, Long commentId, String content) {
        if (image == null || image.getUserId() == null || actorUserId == null || image.getUserId().equals(actorUserId)) {
            return;
        }
        Notification notification = baseNotification(image, actorUserId, "COMMENT");
        notification.setCommentId(commentId);
        notification.setContentPreview(truncate(content));
        notificationMapper.insert(notification);
    }

    @Override
    @Transactional
    public void createLikeNotification(Image image, Long actorUserId) {
        if (image == null || image.getUserId() == null || actorUserId == null || image.getUserId().equals(actorUserId)) {
            return;
        }
        notificationMapper.insert(baseNotification(image, actorUserId, "LIKE"));
    }

    @Override
    @Transactional
    public void createCommentLikeNotification(Comment comment, Long actorUserId) {
        if (comment == null || comment.getUserId() == null || actorUserId == null
                || comment.getUserId().equals(actorUserId)) {
            return;
        }
        Notification notification = baseNotification(comment.getImageId(), comment.getUserId(), actorUserId, "COMMENT_LIKE");
        notification.setCommentId(comment.getId());
        notification.setContentPreview(truncate(comment.getContent()));
        notificationMapper.insert(notification);
    }

    @Override
    public Page<NotificationVO> listMine(Integer page, Integer limit, boolean unreadOnly) {
        long loginId = StpUtil.getLoginIdAsLong();
        Page<NotificationVO> result = notificationMapper.selectNotificationVOPage(
                new Page<>(normalizePage(page), normalizeLimit(limit)), loginId, unreadOnly);

        List<NotificationVO> records = result.getRecords();
        if (!records.isEmpty()) {
            // Batch fetch actor users
            Set<Long> actorIds = records.stream()
                    .map(NotificationVO::getActorUserId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            Map<Long, User> actorsById = actorIds.isEmpty()
                    ? Map.of()
                    : userMapper.selectBatchIds(actorIds).stream()
                            .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

            for (NotificationVO vo : records) {
                decorate(vo, actorsById);
            }
        }
        return result;
    }

    @Override
    public Long unreadCount() {
        Long count = notificationMapper.countUnread(StpUtil.getLoginIdAsLong());
        return count == null ? 0L : count;
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        notificationMapper.markRead(id, StpUtil.getLoginIdAsLong());
    }

    @Override
    @Transactional
    public void markAllRead() {
        notificationMapper.markAllRead(StpUtil.getLoginIdAsLong());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (!notification.getRecipientUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        notificationMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        long userId = StpUtil.getLoginIdAsLong();
        notificationMapper.deleteBatch(ids, userId);
    }

    private Notification baseNotification(Image image, Long actorUserId, String type) {
        return baseNotification(image.getId(), image.getUserId(), actorUserId, type);
    }

    private Notification baseNotification(Long imageId, Long recipientUserId, Long actorUserId, String type) {
        Notification notification = new Notification();
        notification.setImageId(imageId);
        notification.setRecipientUserId(recipientUserId);
        notification.setActorUserId(actorUserId);
        notification.setType(type);
        notification.setReadFlag(0);
        notification.setCreateTime(LocalDateTime.ofInstant(clock.instant(), DATABASE_ZONE));
        return notification;
    }

    private void decorate(NotificationVO vo, Map<Long, User> actorsById) {
        vo.setCreateTime(toInstant(vo.getStoredCreateTime()));
        User actor = vo.getActorUserId() == null ? null : actorsById.get(vo.getActorUserId());
        if (actor != null) {
            String displayName = actor.getDisplayName();
            vo.setActorName(displayName != null && !displayName.isBlank() ? displayName : actor.getUsername());
            vo.setActorUsername(actor.getUsername());
            if (actor.getAvatarKey() != null && !actor.getAvatarKey().isBlank()) {
                vo.setActorAvatarUrl(mediaUrlUtil.userAvatarUrl(actor.getUuid(), actor.getAvatarKey()));
            } else if (actor.getAvatar() != null && !actor.getAvatar().isBlank()) {
                vo.setActorAvatarUrl(actor.getAvatar());
            }
        } else {
            vo.setActorName("已注销用户");
        }
        if (vo.getImageStorageKey() != null && !vo.getImageStorageKey().isBlank()) {
            vo.setImagePreviewUrl(imageUrlService.getPrivateImageUrl(vo.getImageStorageKey()));
        } else if (vo.getImageUuid() != null && !vo.getImageUuid().isBlank()) {
            vo.setImagePreviewUrl("/api/image/download/" + vo.getImageUuid());
        }
        vo.setTargetUrl(buildTargetUrl(vo));
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(DATABASE_ZONE).toInstant();
    }

    private String buildTargetUrl(NotificationVO vo) {
        if (vo.getImageUuid() == null || vo.getImageUuid().isBlank()) {
            return "";
        }
        StringBuilder url = new StringBuilder("/image/").append(vo.getImageUuid())
                .append("?notificationId=").append(vo.getId());
        if (vo.getCommentId() != null) {
            url.append("&commentId=").append(vo.getCommentId());
        }
        if ("COMMENT".equals(vo.getType()) || "COMMENT_LIKE".equals(vo.getType())) {
            url.append("&highlight=comment");
        } else if ("LIKE".equals(vo.getType())) {
            url.append("&highlight=like");
        }
        return url.toString();
    }

    private String truncate(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        return trimmed.length() <= PREVIEW_LIMIT ? trimmed : trimmed.substring(0, PREVIEW_LIMIT);
    }

    private long normalizePage(Integer page) {
        return page != null && page > 0 ? page : 1;
    }

    private long normalizeLimit(Integer limit) {
        return limit != null && limit > 0 && limit <= 50 ? limit : 20;
    }
}
