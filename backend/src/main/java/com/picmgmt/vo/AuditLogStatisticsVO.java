package com.picmgmt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogStatisticsVO {
    private long todayOperationCount;
    private long todayFailedCount;
    private long highRiskCount;
    private long averageCostTime;
}
