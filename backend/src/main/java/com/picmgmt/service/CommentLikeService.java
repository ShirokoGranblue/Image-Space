package com.picmgmt.service;

import com.picmgmt.vo.CommentLikeStatusVO;

public interface CommentLikeService {

    CommentLikeStatusVO like(Long commentId);

    CommentLikeStatusVO unlike(Long commentId);
}
