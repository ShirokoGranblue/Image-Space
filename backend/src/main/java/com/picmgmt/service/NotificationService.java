package com.picmgmt.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.entity.Image;
import com.picmgmt.vo.NotificationVO;

public interface NotificationService {

    void createCommentNotification(Image image, Long actorUserId, Long commentId, String content);

    void createLikeNotification(Image image, Long actorUserId);

    Page<NotificationVO> listMine(Integer page, Integer limit, boolean unreadOnly);

    Long unreadCount();

    void markRead(Long id);

    void markAllRead();

    void delete(Long id);
}
