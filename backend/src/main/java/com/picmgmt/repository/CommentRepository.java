package com.picmgmt.repository;

import com.picmgmt.entity.Comment;
import com.picmgmt.mapper.CommentLikeMapper;
import com.picmgmt.mapper.CommentMapper;
import com.picmgmt.storage.StorageService;
import com.picmgmt.util.MediaUrlUtil;
import com.picmgmt.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final CommentMapper commentMapper;
    private final StorageService storageService;
    private final CommentLikeMapper commentLikeMapper;
    private final MediaUrlUtil mediaUrlUtil;

    public void insert(Comment comment) {
        commentMapper.insert(comment);
    }

    public void deleteById(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        commentMapper.deleteById(commentId);
        if (comment != null) {
            commentLikeMapper.deleteByCommentId(commentId);
            if (comment.getImageKey() != null) {
                storageService.delete("comments", comment.getImageKey());
            }
        }
    }

    public List<CommentVO> listByImageId(Long imageId) {
        List<CommentVO> list = commentMapper.selectCommentVOList(imageId);
        for (CommentVO vo : list) {
            if (vo.getImagePath() != null) {
                vo.setImageUrl(mediaUrlUtil.commentImageUrl(vo.getId(), vo.getImagePath()));
            }
        }
        return list;
    }

    public Comment findById(Long id) {
        return commentMapper.selectById(id);
    }
}
