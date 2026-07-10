package com.picmgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.dto.AuditLogQueryDTO;
import com.picmgmt.entity.AuditLog;
import com.picmgmt.mapper.AuditLogMapper;
import com.picmgmt.audit.AuditLogEventPublisher;
import com.picmgmt.service.AuditLogService;
import com.picmgmt.vo.AuditLogStatisticsVO;
import com.picmgmt.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogMapper auditLogMapper;
    private final AuditLogEventPublisher eventPublisher;

    @Override
    public Page<AuditLogVO> page(AuditLogQueryDTO query) {
        int pageNum = normalizePageNum(query.getPageNum());
        int pageSize = normalizePageSize(query.getPageSize());
        Page<AuditLog> page = auditLogMapper.selectPage(new Page<>(pageNum, pageSize), buildWrapper(query));
        Page<AuditLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(AuditLogVO::from).toList());
        return result;
    }

    @Override
    public AuditLogVO detail(Long id) {
        AuditLog log = auditLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审计日志不存在");
        }
        return AuditLogVO.from(log);
    }

    @Override
    public AuditLogStatisticsVO statistics() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<AuditLog> today = new LambdaQueryWrapper<AuditLog>()
                .ge(AuditLog::getCreateTime, startOfDay);
        long todayCount = auditLogMapper.selectCount(today);
        long failedCount = auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLog>()
                .ge(AuditLog::getCreateTime, startOfDay)
                .and(wrapper -> wrapper.eq(AuditLog::getStatus, "FAILED").or().eq(AuditLog::getResult, "FAIL")));
        long highRiskCount = auditLogMapper.selectCount(new LambdaQueryWrapper<AuditLog>()
                .ge(AuditLog::getCreateTime, startOfDay)
                .eq(AuditLog::getRiskLevel, "HIGH"));
        List<AuditLog> logs = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                .ge(AuditLog::getCreateTime, startOfDay)
                .select(AuditLog::getCostTime));
        long averageCostTime = Math.round(logs.stream()
                .map(AuditLog::getCostTime)
                .filter(value -> value != null && value >= 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0));
        return new AuditLogStatisticsVO(todayCount, failedCount, highRiskCount, averageCostTime);
    }

    @Override
    public List<AuditLogVO> recentRisk() {
        return auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                        .and(wrapper -> wrapper.eq(AuditLog::getRiskLevel, "HIGH")
                                .or().eq(AuditLog::getStatus, "FAILED")
                                .or().eq(AuditLog::getResult, "FAIL"))
                        .orderByDesc(AuditLog::getCreateTime)
                        .last("LIMIT 20"))
                .stream()
                .map(AuditLogVO::from)
                .toList();
    }

    @Override
    public SseEmitter stream() {
        return eventPublisher.subscribe();
    }

    private LambdaQueryWrapper<AuditLog> buildWrapper(AuditLogQueryDTO query) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(hasText(query.getUsername()), AuditLog::getUsername, query.getUsername());
        wrapper.eq(hasText(query.getModule()), AuditLog::getModule, query.getModule());
        wrapper.eq(hasText(query.getOperationType()), AuditLog::getAction, query.getOperationType());
        if (hasText(query.getStatus())) {
            String status = normalizeStatus(query.getStatus());
            wrapper.and(w -> w.eq(AuditLog::getStatus, status)
                    .or("FAILED".equals(status), ww -> ww.eq(AuditLog::getResult, "FAIL")));
        }
        wrapper.eq(hasText(query.getRiskLevel()), AuditLog::getRiskLevel, query.getRiskLevel());
        wrapper.like(hasText(query.getIp()), AuditLog::getIp, query.getIp());
        wrapper.like(hasText(query.getPath()), AuditLog::getPath, query.getPath());
        LocalDateTime startTime = parseDateTime(query.getStartTime());
        LocalDateTime endTime = parseDateTime(query.getEndTime());
        wrapper.ge(startTime != null, AuditLog::getCreateTime, startTime);
        wrapper.le(endTime != null, AuditLog::getCreateTime, endTime);
        wrapper.orderByDesc(AuditLog::getCreateTime);
        return wrapper;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeStatus(String status) {
        if ("FAIL".equalsIgnoreCase(status)) {
            return "FAILED";
        }
        return status.toUpperCase();
    }

    private LocalDateTime parseDateTime(String value) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.contains("T")) {
                return LocalDateTime.parse(trimmed);
            }
            return LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
