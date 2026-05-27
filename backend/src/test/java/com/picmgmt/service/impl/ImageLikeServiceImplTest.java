package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.entity.Image;
import com.picmgmt.entity.ImageLike;
import com.picmgmt.mapper.ImageLikeMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.service.NotificationService;
import com.picmgmt.vo.ImageLikeStatusVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageLikeServiceImplTest {

    @Mock private ImageLikeMapper imageLikeMapper;
    @Mock private ImageRepository imageRepository;
    @Mock private NotificationService notificationService;

    private ImageLikeServiceImpl service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new ImageLikeServiceImpl(imageLikeMapper, imageRepository, notificationService);
        stpMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void like_shouldCreateLikeAndNotifyOwnerWhenOtherUserLikes() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
        Image image = new Image();
        image.setId(5L);
        image.setUserId(1L);
        image.setImageName("summer.jpg");
        when(imageRepository.findById(5L)).thenReturn(Optional.of(image));
        when(imageLikeMapper.findByImageIdAndUserId(5L, 2L)).thenReturn(null);
        when(imageLikeMapper.countByImageId(5L)).thenReturn(1L);

        ImageLikeStatusVO status = service.like(5L);

        assertTrue(status.getLikedByMe());
        assertEquals(1L, status.getLikeCount());
        verify(imageLikeMapper).insert(any(ImageLike.class));
        verify(notificationService).createLikeNotification(image, 2L);
    }

    @Test
    void like_shouldNotInsertOrNotifyWhenAlreadyLiked() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
        Image image = new Image();
        image.setId(5L);
        image.setUserId(1L);
        when(imageRepository.findById(5L)).thenReturn(Optional.of(image));
        when(imageLikeMapper.findByImageIdAndUserId(5L, 2L)).thenReturn(new ImageLike());
        when(imageLikeMapper.countByImageId(5L)).thenReturn(1L);

        ImageLikeStatusVO status = service.like(5L);

        assertTrue(status.getLikedByMe());
        assertEquals(1L, status.getLikeCount());
        verify(imageLikeMapper, never()).insert(any(ImageLike.class));
        verify(notificationService, never()).createLikeNotification(any(), anyLong());
    }

    @Test
    void like_shouldNotNotifyWhenOwnerLikesOwnImage() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        Image image = new Image();
        image.setId(5L);
        image.setUserId(1L);
        when(imageRepository.findById(5L)).thenReturn(Optional.of(image));
        when(imageLikeMapper.findByImageIdAndUserId(5L, 1L)).thenReturn(null);
        when(imageLikeMapper.countByImageId(5L)).thenReturn(1L);

        service.like(5L);

        verify(notificationService, never()).createLikeNotification(any(), anyLong());
    }

    @Test
    void unlike_shouldDeleteLikeAndReturnUpdatedStatus() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
        Image image = new Image();
        image.setId(5L);
        image.setUserId(1L);
        when(imageRepository.findById(5L)).thenReturn(Optional.of(image));
        when(imageLikeMapper.countByImageId(5L)).thenReturn(0L);

        ImageLikeStatusVO status = service.unlike(5L);

        assertEquals(false, status.getLikedByMe());
        assertEquals(0L, status.getLikeCount());
        verify(imageLikeMapper).deleteByImageIdAndUserId(5L, 2L);
    }
}
