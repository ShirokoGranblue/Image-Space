package com.picmgmt.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.picmgmt.entity.Comment;
import com.picmgmt.mapper.CommentMapper;
import com.picmgmt.mapper.ImageMapper;
import com.picmgmt.service.CommentService;
import com.picmgmt.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ImageMapper imageMapper;

    @Override
    public Comment add(Long imageId, String content, String imagePath) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
        if (imageMapper.selectById(imageId) == null) {
            throw new IllegalArgumentException("图片不存在");
        }
        Comment comment = new Comment();
        comment.setImageId(imageId);
        comment.setUserId(StpUtil.getLoginIdAsLong());
        comment.setContent(content);
        comment.setImagePath(imagePath);
        commentMapper.insert(comment);
        return comment;
    }

    @Override
    public void delete(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        long userId = StpUtil.getLoginIdAsLong();
        if (!StpUtil.hasRole("admin") && !comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该评论");
        }
        commentMapper.deleteById(commentId);
    }

    @Override
    public List<CommentVO> listByImage(Long imageId) {
        return commentMapper.selectCommentVOList(imageId);
    }
}
