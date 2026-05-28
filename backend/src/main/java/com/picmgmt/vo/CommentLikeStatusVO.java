package com.picmgmt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeStatusVO {

    private Long commentId;
    private Long likeCount;
    private Boolean likedByMe;
}
