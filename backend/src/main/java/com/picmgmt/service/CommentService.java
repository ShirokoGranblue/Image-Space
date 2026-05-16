package com.picmgmt.service;

import com.picmgmt.entity.Comment;
import com.picmgmt.vo.CommentVO;

import java.util.List;

public interface CommentService {

    Comment add(Long imageId, String content, String imagePath);

    void delete(Long commentId);

    List<CommentVO> listByImage(Long imageId);
}
