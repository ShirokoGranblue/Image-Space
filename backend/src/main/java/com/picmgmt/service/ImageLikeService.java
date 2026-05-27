package com.picmgmt.service;

import com.picmgmt.vo.ImageLikeStatusVO;

public interface ImageLikeService {

    ImageLikeStatusVO like(Long imageId);

    ImageLikeStatusVO unlike(Long imageId);
}
