package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.entity.Comment;
import com.picmgmt.entity.Image;
import com.picmgmt.image.ImagePermissionService;
import com.picmgmt.like.LikeTarget;
import com.picmgmt.mapper.CommentLikeMapper;
import com.picmgmt.mapper.ImageLikeMapper;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.service.CommentService;
import com.picmgmt.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock private ImageLikeMapper imageLikeMapper;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private ImageRepository imageRepository;
    @Mock private CommentService commentService;
    @Mock private NotificationService notificationService;
    @Mock private ImagePermissionService imagePermissionService;

    @InjectMocks
    private LikeServiceImpl service;

    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        stpMock = mockStatic(StpUtil.class);
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(9L);
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void likeImageRejectsImageThatCallerCannotView() {
        Image image = image(7L);
        when(imageRepository.findById(7L)).thenReturn(Optional.of(image));
        when(imagePermissionService.canView(image)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.like(LikeTarget.IMAGE, 7L));

        verify(imageLikeMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void likeCommentRejectsCommentWhoseParentImageIsNotVisible() {
        Comment comment = new Comment();
        comment.setId(11L);
        comment.setImageId(7L);
        comment.setUserId(4L);
        Image image = image(7L);
        when(commentService.getById(11L)).thenReturn(comment);
        when(imageRepository.findById(7L)).thenReturn(Optional.of(image));
        when(imagePermissionService.canView(image)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.like(LikeTarget.COMMENT, 11L));

        verify(commentLikeMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unlikeImageRejectsImageThatCallerCannotView() {
        Image image = image(7L);
        when(imageRepository.findById(7L)).thenReturn(Optional.of(image));
        when(imagePermissionService.canView(image)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.unlike(LikeTarget.IMAGE, 7L));

        verify(imageLikeMapper, never()).deleteByImageIdAndUserId(7L, 9L);
    }

    private Image image(Long id) {
        Image image = new Image();
        image.setId(id);
        image.setUserId(4L);
        image.setVisibility("PRIVATE");
        return image;
    }
}
