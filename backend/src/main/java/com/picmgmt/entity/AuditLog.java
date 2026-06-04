package com.picmgmt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String module;
    private String targetType;
    private String targetId;
    private String method;
    private String path;
    private String ip;
    private String userAgent;
    private String requestParams;
    private String result;
    private String errorMessage;
    private LocalDateTime createTime;
}
