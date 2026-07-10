package com.picmgmt.vo;

import com.picmgmt.entity.AuditLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String operationType;
    private String action;
    private String method;
    private String path;
    private String ip;
    private String userAgent;
    private String requestParams;
    private String responseResult;
    private String errorMsg;
    private String status;
    private String riskLevel;
    private Long costTime;
    private LocalDateTime createTime;

    public static AuditLogVO from(AuditLog log) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setModule(log.getModule());
        vo.setOperationType(log.getAction());
        vo.setAction(log.getAction());
        vo.setMethod(log.getMethod());
        vo.setPath(log.getPath());
        vo.setIp(log.getIp());
        vo.setUserAgent(log.getUserAgent());
        vo.setRequestParams(log.getRequestParams());
        vo.setResponseResult(log.getResponseResult());
        vo.setErrorMsg(log.getErrorMessage());
        vo.setStatus(normalizeStatus(log.getStatus(), log.getResult()));
        vo.setRiskLevel(log.getRiskLevel() == null ? "LOW" : log.getRiskLevel());
        vo.setCostTime(log.getCostTime() == null ? 0L : log.getCostTime());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private static String normalizeStatus(String status, String result) {
        if (status != null && !status.isBlank()) {
            return status;
        }
        return "FAIL".equalsIgnoreCase(result) ? "FAILED" : "SUCCESS";
    }
}
