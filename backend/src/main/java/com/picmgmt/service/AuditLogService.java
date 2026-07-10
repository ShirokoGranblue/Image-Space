package com.picmgmt.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.dto.AuditLogQueryDTO;
import com.picmgmt.vo.AuditLogStatisticsVO;
import com.picmgmt.vo.AuditLogVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AuditLogService {
    Page<AuditLogVO> page(AuditLogQueryDTO query);

    AuditLogVO detail(Long id);

    AuditLogStatisticsVO statistics();

    List<AuditLogVO> recentRisk();

    SseEmitter stream();
}
