package com.picmgmt.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileVO extends UserVO {

    private Long publicLikeCount;
}
