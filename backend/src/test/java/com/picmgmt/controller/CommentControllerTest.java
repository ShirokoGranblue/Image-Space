package com.picmgmt.controller;

import com.picmgmt.entity.Comment;
import com.picmgmt.service.CommentService;
import com.picmgmt.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock private CommentService commentService;
    @Mock private StorageService storageService;
    @InjectMocks private CommentController controller;

    @Test
    void imageUsesMigratedImageKeyWhenPresent() {
        Comment comment = new Comment();
        comment.setId(7L);
        comment.setImagePath("data:image/png;base64,legacy");
        comment.setImageKey("comments/current.png");
        byte[] expected = new byte[]{1, 2, 3};
        when(commentService.getById(7L)).thenReturn(comment);
        when(storageService.download(eq("comments"), anyString())).thenReturn(expected);

        assertArrayEquals(expected, controller.image(7L).getBody());
        verify(storageService).download("comments", "comments/current.png");
    }
}
