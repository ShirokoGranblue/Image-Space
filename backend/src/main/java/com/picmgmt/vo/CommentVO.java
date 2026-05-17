package com.picmgmt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {

    private Long id;
    private Long imageId;
    private Long userId;
    private String username;
    private String displayName;
    private String content;
    private String imagePath;
    private String imageUrl;
    private LocalDateTime createTime;
}
