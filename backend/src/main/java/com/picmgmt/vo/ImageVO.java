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
    private String publicUrl;
    private String privateUrl;
    private String storageKey;
    private String originalUrl;
    private String originalKey;
    private String originalFilename;
    private String originalContentType;
    private String originalExt;
    private Long originalSize;
    private Integer width;
    private Integer height;
    private String mediumUrl;
    private String mediumKey;
    private String thumbUrl;
    private String thumbKey;
    private Long fileSize;
    private String imageType;
    private String description;
    private String tags;
    private String visibility;
    private Long mediaVersion;
    private String visibleUsernames;
    private Boolean ownedByMe;
    private Boolean editableByMe;
    private Long likeCount;
    private Long commentCount;
    private Boolean likedByMe;
    private LocalDateTime uploadTime;
}
