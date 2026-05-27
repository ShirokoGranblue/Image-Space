package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.Notification;
import com.picmgmt.entity.User;
import com.picmgmt.mapper.NotificationMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.vo.NotificationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationMapper notificationMapper;
    @Mock private UserMapper userMapper;
    @Mock private StorageService storageService;

    private NotificationServiceImpl service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationMapper, userMapper, storageService);
        stpMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void createCommentNotification_shouldStoreRecipientActorImageAndPreview() {
        Image image = new Image();
        image.setId(7L);
        image.setUserId(1L);
        image.setImageName("summer.jpg");
        image.setStorageKey("1/summer.jpg");

        service.createCommentNotification(image, 2L, 11L, "a".repeat(100));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification notification = captor.getValue();
        assertEquals(1L, notification.getRecipientUserId());
        assertEquals(2L, notification.getActorUserId());
        assertEquals(7L, notification.getImageId());
        assertEquals(11L, notification.getCommentId());
        assertEquals("COMMENT", notification.getType());
        assertEquals(80, notification.getContentPreview().length());
    }

    @Test
    void createLikeNotification_shouldStoreLikeNotification() {
        Image image = new Image();
        image.setId(7L);
        image.setUserId(1L);
        image.setImageName("summer.jpg");

        service.createLikeNotification(image, 2L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals("LIKE", captor.getValue().getType());
        assertEquals(1L, captor.getValue().getRecipientUserId());
    }

    @Test
    void listMine_shouldUseLoginIdAndDecorateActorAndImagePreview() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        Page<NotificationVO> page = new Page<>(1, 20);
        NotificationVO vo = new NotificationVO();
        vo.setId(9L);
        vo.setActorUserId(2L);
        vo.setImageId(7L);
        vo.setImageStorageKey("1/summer.jpg");
        page.setRecords(java.util.List.of(vo));
        when(notificationMapper.selectNotificationVOPage(any(), eq(1L), eq(false))).thenReturn(page);
        User actor = new User();
        actor.setId(2L);
        actor.setUsername("alice");
        actor.setDisplayName("Alice");
        actor.setAvatarKey("2/avatar.png");
        when(userMapper.selectById(2L)).thenReturn(actor);
        when(storageService.getAccessUrl("avatars", "2/avatar.png")).thenReturn("/api/user/avatar/2");
        when(storageService.getAccessUrl("images", "1/summer.jpg")).thenReturn("/api/image/download/7");

        Page<NotificationVO> result = service.listMine(1, 20, false);

        NotificationVO resultVo = result.getRecords().get(0);
        assertEquals("Alice", resultVo.getActorName());
        assertEquals("/api/user/avatar/2", resultVo.getActorAvatarUrl());
        assertEquals("/api/image/download/7", resultVo.getImagePreviewUrl());
        assertEquals("/image/7?notificationId=9", resultVo.getTargetUrl());
    }
}
