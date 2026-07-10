package com.picmgmt.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.Result;
import com.picmgmt.audit.AuditStreamTicketService;
import com.picmgmt.dto.AuditLogQueryDTO;
import com.picmgmt.service.AuditLogService;
import com.picmgmt.vo.AuditLogStatisticsVO;
import com.picmgmt.vo.AuditLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Tag(name = "后台审计日志")
@RestController
@RequestMapping("/admin/audit-log")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogService auditLogService;
    private final AuditStreamTicketService streamTicketService;

    @Operation(summary = "分页查询审计日志")
    @GetMapping("/page")
    public Result<Page<AuditLogVO>> page(AuditLogQueryDTO query) {
        checkAdmin();
        return Result.ok(auditLogService.page(query));
    }

    @Operation(summary = "查询审计日志详情")
    @GetMapping("/{id}")
    public Result<AuditLogVO> detail(@PathVariable Long id) {
        checkAdmin();
        return Result.ok(auditLogService.detail(id));
    }

    @Operation(summary = "查询审计日志统计")
    @GetMapping("/statistics")
    public Result<AuditLogStatisticsVO> statistics() {
        checkAdmin();
        return Result.ok(auditLogService.statistics());
    }

    @Operation(summary = "查询最近高风险审计日志")
    @GetMapping("/recent-risk")
    public Result<List<AuditLogVO>> recentRisk() {
        checkAdmin();
        return Result.ok(auditLogService.recentRisk());
    }

    @Operation(summary = "实时推送高风险或失败审计日志")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String ticket) {
        streamTicketService.consume(ticket);
        return auditLogService.stream();
    }

    @Operation(summary = "签发一次性审计流凭证")
    @PostMapping("/stream-ticket")
    public Result<Map<String, String>> streamTicket() {
        checkAdmin();
        return Result.ok(Map.of(
                "ticket",
                streamTicketService.issue(StpUtil.getLoginIdAsLong())
        ));
    }

    private void checkAdmin() {
        StpUtil.checkLogin();
        StpUtil.checkRole("admin");
    }
}
