package com.picmgmt.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;
    private String type;
    private Long recipientUserId;
    private Long actorUserId;
    private String actorName;
    private String actorUsername;
    private String actorAvatarUrl;
    private Long imageId;
    private String imageUuid;
    private String imageName;
    private String imageStorageKey;
    private String imagePreviewUrl;
    private Long commentId;
    private String contentPreview;
    private Boolean read;
    @JsonIgnore
    private LocalDateTime storedCreateTime;
    private Instant createTime;
    private String targetUrl;
}
