package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.Notification;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.NotificationMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.service.NotificationService;
import com.picmgmt.storage.StorageService;
import com.picmgmt.util.MediaUrlUtil;
import com.picmgmt.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int PREVIEW_LIMIT = 80;

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final StorageService storageService;

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
    public Page<NotificationVO> listMine(Integer page, Integer limit, boolean unreadOnly) {
        long loginId = StpUtil.getLoginIdAsLong();
        Page<NotificationVO> result = notificationMapper.selectNotificationVOPage(
                new Page<>(normalizePage(page), normalizeLimit(limit)), loginId, unreadOnly);
        for (NotificationVO vo : result.getRecords()) {
            decorate(vo);
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

    private Notification baseNotification(Image image, Long actorUserId, String type) {
        Notification notification = new Notification();
        notification.setRecipientUserId(image.getUserId());
        notification.setActorUserId(actorUserId);
        notification.setImageId(image.getId());
        notification.setType(type);
        notification.setReadFlag(0);
        notification.setCreateTime(LocalDateTime.now());
        return notification;
    }

    private void decorate(NotificationVO vo) {
        User actor = vo.getActorUserId() == null ? null : userMapper.selectById(vo.getActorUserId());
        if (actor != null) {
            String displayName = actor.getDisplayName();
            vo.setActorName(displayName != null && !displayName.isBlank() ? displayName : actor.getUsername());
            if (actor.getAvatarKey() != null && !actor.getAvatarKey().isBlank()) {
                vo.setActorAvatarUrl(MediaUrlUtil.userMediaUrl("avatar", actor.getId(), actor.getAvatarKey()));
            } else if (actor.getAvatar() != null && !actor.getAvatar().isBlank()) {
                vo.setActorAvatarUrl(actor.getAvatar());
            }
        } else {
            vo.setActorName("已注销用户");
        }
        if (vo.getImageStorageKey() != null && !vo.getImageStorageKey().isBlank()) {
            vo.setImagePreviewUrl(MediaUrlUtil.imageDownloadUrl(vo.getImageId(), vo.getImageStorageKey()));
        } else if (vo.getImageId() != null) {
            vo.setImagePreviewUrl("/api/image/download/" + vo.getImageId());
        }
        vo.setTargetUrl(buildTargetUrl(vo));
    }

    private String buildTargetUrl(NotificationVO vo) {
        StringBuilder url = new StringBuilder("/image/").append(vo.getImageId())
                .append("?notificationId=").append(vo.getId());
        if (vo.getCommentId() != null) {
            url.append("&commentId=").append(vo.getCommentId());
        }
        if ("COMMENT".equals(vo.getType())) {
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
