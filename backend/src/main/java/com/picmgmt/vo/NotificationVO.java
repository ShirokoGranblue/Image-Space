package com.picmgmt.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;
    private String type;
    private Long recipientUserId;
    private Long actorUserId;
    private String actorName;
    private String actorAvatarUrl;
    private Long imageId;
    private String imageName;
    private String imageStorageKey;
    private String imagePreviewUrl;
    private Long commentId;
    private String contentPreview;
    private Boolean read;
    private LocalDateTime createTime;
    private String targetUrl;
}
