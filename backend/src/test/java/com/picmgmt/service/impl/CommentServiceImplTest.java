package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.entity.Comment;
import com.picmgmt.entity.Image;
import com.picmgmt.repository.CommentRepository;
import com.picmgmt.repository.ImageRepository;
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

    private CommentServiceImpl service;
    private MockedStatic<StpUtil> stpMock;

    @BeforeEach
    void setUp() {
        service = new CommentServiceImpl(commentRepository, imageRepository);
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
        doNothing().when(commentRepository).insert(any(Comment.class));

        Comment result = service.add(1L, "nice pic", null);

        assertNotNull(result);
        assertEquals("nice pic", result.getContent());
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getImageId());
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
        when(commentRepository.findById(1L)).thenReturn(comment);
        doNothing().when(commentRepository).deleteById(anyLong());

        assertDoesNotThrow(() -> service.delete(1L));
        verify(commentRepository).deleteById(1L);
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
