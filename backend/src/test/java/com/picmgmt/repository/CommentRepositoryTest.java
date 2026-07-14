package com.picmgmt.repository;

import com.picmgmt.mapper.CommentLikeMapper;
import com.picmgmt.mapper.CommentMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.util.MediaUrlUtil;
import com.picmgmt.vo.CommentVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentRepositoryTest {

    @Mock private CommentMapper commentMapper;
    @Mock private StorageService storageService;
    @Mock private CommentLikeMapper commentLikeMapper;
    @Mock private MediaUrlUtil mediaUrlUtil;

    @Test
    void listByImageId_shouldExposeCommentImageThroughBackendEndpoint() {
        CommentVO comment = new CommentVO();
        comment.setId(42L);
        comment.setImagePath("comments/example.png");
        when(commentMapper.selectCommentVOList(7L)).thenReturn(List.of(comment));
        when(mediaUrlUtil.commentImageUrl(42L, "comments/example.png"))
                .thenReturn("/api/comment/image/42?v=64537f941595");
        CommentRepository repository = new CommentRepository(
                commentMapper, storageService, commentLikeMapper, mediaUrlUtil);

        List<CommentVO> result = repository.listByImageId(7L);

        assertEquals("/api/comment/image/42?v=64537f941595", result.get(0).getImageUrl());
        verify(mediaUrlUtil).commentImageUrl(42L, "comments/example.png");
    }
}
