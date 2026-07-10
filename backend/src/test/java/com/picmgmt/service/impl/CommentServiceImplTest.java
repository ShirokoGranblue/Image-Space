package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.entity.Comment;
import com.picmgmt.entity.Image;
import com.picmgmt.mapper.CommentLikeMapper;
import com.picmgmt.image.ImagePermissionService;
import com.picmgmt.repository.CommentRepository;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.service.NotificationService;
import com.picmgmt.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private NotificationService notificationService;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private ImagePermissionService imagePermissionService;
    @Mock private StorageService storageService;

    private CommentServiceImpl service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new CommentServiceImpl(
                commentRepository,
                imageRepository,
                notificationService,
                commentLikeMapper,
                imagePermissionService,
                storageService
        );
        stpMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpMock.close();
    }

    @Test
    void add_shouldThrowForNullContent() {
        try {
            service.add(1L, null, null);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(4002, e.getErrorCode().getCode());
        }
    }

    @Test
    void add_shouldThrowForBlankContent() {
        try {
            service.add(1L, "   ", null);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(4002, e.getErrorCode().getCode());
        }
    }

    @Test
    void add_shouldThrowWhenImageNotFound() {
        when(imageRepository.findById(anyLong())).thenReturn(Optional.empty());

        try {
            service.add(99L, "nice pic", null);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(1001, e.getErrorCode().getCode());
        }
    }

    @Test
    void add_shouldSucceedForValidContent() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        Image image = new Image();
        image.setId(1L);
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(imagePermissionService.canView(image)).thenReturn(true);
        doNothing().when(commentRepository).insert(any(Comment.class));

        Comment result = service.add(1L, "nice pic", null);

        assertNotNull(result);
        assertEquals("nice pic", result.getContent());
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getImageId());
    }

    @Test
    void add_shouldNotifyImageOwnerWhenOtherUserComments() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(2L);

        Image image = new Image();
        image.setId(1L);
        image.setUserId(1L);
        image.setImageName("summer.jpg");
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(imagePermissionService.canView(image)).thenReturn(true);
        doAnswer(invocation -> {
            Comment comment = invocation.getArgument(0, Comment.class);
            comment.setId(10L);
            return null;
        }).when(commentRepository).insert(any(Comment.class));

        service.add(1L, "nice pic", null);

        verify(notificationService).createCommentNotification(image, 2L, 10L, "nice pic");
    }

    @Test
    void add_shouldNotNotifyWhenOwnerCommentsOwnImage() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        Image image = new Image();
        image.setId(1L);
        image.setUserId(1L);
        image.setImageName("summer.jpg");
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(imagePermissionService.canView(image)).thenReturn(true);

        service.add(1L, "self note", null);

        verify(notificationService, never()).createCommentNotification(any(), anyLong(), anyLong(), any());
    }

    @Test
    void add_shouldStoreUploadedObjectAsImageKey() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        Image image = new Image();
        image.setId(1L);
        image.setUserId(1L);
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(imagePermissionService.canView(image)).thenReturn(true);

        Comment result = service.add(1L, "with image", "comments/key.png");

        assertEquals("comments/key.png", result.getImageKey());
    }

    @Test
    void add_shouldDeleteUploadedObjectWhenInsertFails() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        Image image = new Image();
        image.setId(1L);
        image.setUserId(1L);
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(imagePermissionService.canView(image)).thenReturn(true);
        doThrow(new RuntimeException("db failed")).when(commentRepository).insert(any(Comment.class));

        assertThrows(RuntimeException.class,
                () -> service.add(1L, "with image", "comments/key.png"));

        verify(storageService).deleteObjectIfExists("comments", "comments/key.png");
    }

    @Test
    void getByIdRejectsCommentImageWhenParentImageIsNotVisible() {
        Comment comment = new Comment();
        comment.setId(3L);
        comment.setImageId(1L);
        Image image = new Image();
        image.setId(1L);
        when(commentRepository.findById(3L)).thenReturn(comment);
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(imagePermissionService.canView(image)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.getById(3L));
    }

    @Test
    void delete_shouldThrowWhenCommentNotFound() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        when(commentRepository.findById(anyLong())).thenReturn(null);

        try {
            service.delete(99L);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(4001, e.getErrorCode().getCode());
        }
    }

    @Test
    void delete_shouldThrowWhenNotOwnerAndNotAdmin() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
        stpMock.when(() -> StpUtil.hasRole("admin")).thenReturn(false);

        Comment comment = new Comment();
        comment.setUserId(1L);
        when(commentRepository.findById(1L)).thenReturn(comment);

        try {
            service.delete(1L);
            fail("Expected BusinessException");
        } catch (BusinessException e) {
            assertEquals(403, e.getErrorCode().getCode());
        }
    }

    @Test
    void delete_shouldSucceedForOwner() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        Comment comment = new Comment();
        comment.setUserId(1L);
        comment.setImageKey("comments/key.png");
        when(commentRepository.findById(1L)).thenReturn(comment);
        doNothing().when(commentRepository).deleteById(anyLong());

        assertDoesNotThrow(() -> service.delete(1L));
        verify(commentRepository).deleteById(1L);
        verify(storageService).deleteObjectIfExists("comments", "comments/key.png");
    }

    @Test
    void delete_shouldSucceedForAdmin() {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
        stpMock.when(() -> StpUtil.hasRole("admin")).thenReturn(true);

        Comment comment = new Comment();
        comment.setUserId(1L);
        when(commentRepository.findById(1L)).thenReturn(comment);
        doNothing().when(commentRepository).deleteById(anyLong());

        assertDoesNotThrow(() -> service.delete(1L));
        verify(commentRepository).deleteById(1L);
    }
}
