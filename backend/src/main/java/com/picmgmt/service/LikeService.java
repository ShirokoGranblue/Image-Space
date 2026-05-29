package com.picmgmt.service;

import com.picmgmt.like.LikeTarget;
import com.picmgmt.vo.LikeStatusVO;

public interface LikeService {
    LikeStatusVO like(LikeTarget target, Long targetId);
    LikeStatusVO unlike(LikeTarget target, Long targetId);
    void deleteAllByTarget(LikeTarget target, Long targetId);
}
