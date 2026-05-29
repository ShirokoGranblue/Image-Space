package com.picmgmt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeStatusVO {
    private Long targetId;
    private Long likeCount;
    private Boolean likedByMe;
}
