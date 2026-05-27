package com.picmgmt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipientUserId;
    private Long actorUserId;
    private Long imageId;
    private Long commentId;
    private String type;
    private String contentPreview;
    private Integer readFlag;
    private LocalDateTime createTime;
}
