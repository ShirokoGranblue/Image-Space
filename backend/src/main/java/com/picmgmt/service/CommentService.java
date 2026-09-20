package com.picmgmt.service;

import com.picmgmt.entity.Comment;
import com.picmgmt.vo.CommentVO;

import java.util.List;

import org.springframework.stereotype.Service;

public interface CommentService {

    Comment add(Long imageId, String content, String imagePath);

    void delete(Long commentId);

    List<CommentVO> listByImage(Long imageId);

    List<CommentVO> listByImageUuid(String imageUuid);

    Comment getById(Long commentId);
}
