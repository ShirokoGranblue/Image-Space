package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.entity.Comment;
import com.picmgmt.repository.CommentRepository;
import com.picmgmt.repository.ImageRepository;
import com.picmgmt.service.CommentService;
import com.picmgmt.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ImageRepository imageRepository;

    @Override
    public Comment add(Long imageId, String content, String imagePath) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.COMMENT_EMPTY);
        }
        imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        Comment comment = new Comment();
        comment.setImageId(imageId);
        comment.setUserId(StpUtil.getLoginIdAsLong());
        comment.setContent(content);
        comment.setImagePath(imagePath);
        commentRepository.insert(comment);
        return comment;
    }

    @Override
    public void delete(Long commentId) {
        Comment comment = commentRepository.findById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        long userId = StpUtil.getLoginIdAsLong();
        if (!StpUtil.hasRole("admin") && !comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentVO> listByImage(Long imageId) {
        return commentRepository.listByImageId(imageId);
    }
}
