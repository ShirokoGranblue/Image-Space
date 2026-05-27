package com.picmgmt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageLikeStatusVO {

    private Long imageId;
    private Long likeCount;
    private Boolean likedByMe;
}
