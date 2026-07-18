package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.Notification;
import com.picmgmt.entity.User;
import com.picmgmt.image.ImageUrlService;
import com.picmgmt.mapper.NotificationMapper;
import com.picmgmt.mapper.UserMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.util.MediaUrlUtil;
import com.picmgmt.vo.NotificationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-16T02:30:00Z");

    @Mock private NotificationMapper notificationMapper;
    @Mock private UserMapper userMapper;
    @Mock private StorageService storageService;
    @Mock private MediaUrlUtil mediaUrlUtil;
    @Mock private ImageUrlService imageUrlService;

    private NotificationServiceImpl service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(
                notificationMapper,
                userMapper,
                storageService,
                mediaUrlUtil,
                imageUrlService,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
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
        assertEquals(LocalDateTime.of(2026, 7, 16, 10, 30), notification.getCreateTime());
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
        vo.setImageUuid("400a1e49-6990-489e-b4a8-35eb0a02d056");
        vo.setImageStorageKey("images/1/summer.jpg");
        vo.setStoredCreateTime(LocalDateTime.of(2026, 7, 16, 10, 30));
        page.setRecords(java.util.List.of(vo));
        when(notificationMapper.selectNotificationVOPage(any(), eq(1L), eq(false))).thenReturn(page);
        User actor = new User();
        actor.setId(2L);
        actor.setUuid("actor-uuid");
        actor.setUsername("alice");
        actor.setDisplayName("Alice");
        actor.setAvatarKey("2/avatar.png");
        when(userMapper.selectBatchIds(java.util.Set.of(2L))).thenReturn(java.util.List.of(actor));

        String expectedAvatarUrl = "/api/user/avatar/actor-uuid?v=e3be9a8665ae";
        String expectedPreviewUrl = "https://cdn.image-space.app/private/images/1/summer.jpg?auth=abc&expires=1893456000";
        when(mediaUrlUtil.userAvatarUrl("actor-uuid", "2/avatar.png")).thenReturn(expectedAvatarUrl);
        when(imageUrlService.getPrivateImageUrl(eq("images/1/summer.jpg")))
                .thenReturn(expectedPreviewUrl);

        Page<NotificationVO> result = service.listMine(1, 20, false);

        NotificationVO resultVo = result.getRecords().get(0);
        assertEquals("Alice", resultVo.getActorName());
        assertEquals("alice", resultVo.getActorUsername());
        assertEquals(expectedAvatarUrl, resultVo.getActorAvatarUrl());
        assertEquals(expectedPreviewUrl, resultVo.getImagePreviewUrl());
        assertEquals("/image/400a1e49-6990-489e-b4a8-35eb0a02d056?notificationId=9", resultVo.getTargetUrl());
        assertEquals(FIXED_NOW, resultVo.getCreateTime());
        verify(imageUrlService).getPrivateImageUrl(eq("images/1/summer.jpg"));
    }

    @Test
    void listMineShouldKeepNullCreateTimeWithoutFailingThePage() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        Page<NotificationVO> page = new Page<>(1, 20);
        NotificationVO vo = new NotificationVO();
        vo.setId(9L);
        page.setRecords(java.util.List.of(vo));
        when(notificationMapper.selectNotificationVOPage(any(), eq(1L), eq(false))).thenReturn(page);

        Page<NotificationVO> result = service.listMine(1, 20, false);

        assertNull(result.getRecords().get(0).getCreateTime());
    }
}
