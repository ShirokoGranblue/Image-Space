package com.picmgmt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImageVO {

    private Long id;
    private String uuid;
    private Long userId;
    private String userUuid;
    private String username;
    private String displayName;
    private Long categoryId;
    private String categoryName;
    private String imageName;
    private String imagePath;
    private String imageUrl;
    private String storageKey;
    private Long fileSize;
    private String imageType;
    private String description;
    private String tags;
    private String visibility;
    private String visibleUsernames;
    private Long likeCount;
    private Boolean likedByMe;
    private LocalDateTime uploadTime;
}
