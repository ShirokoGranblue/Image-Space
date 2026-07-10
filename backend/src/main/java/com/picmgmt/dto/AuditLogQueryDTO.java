package com.picmgmt.dto;

import lombok.Data;

@Data
public class AuditLogQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String username;
    private String module;
    private String operationType;
    private String status;
    private String riskLevel;
    private String ip;
    private String path;
    private String startTime;
    private String endTime;
}
