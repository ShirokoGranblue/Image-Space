package com.picmgmt.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picmgmt.common.Result;
import com.picmgmt.service.NotificationService;
import com.picmgmt.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "通知模块")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取我的通知列表")
    @GetMapping("/list")
    public Result<Page<NotificationVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "20") Integer limit,
                                             @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return Result.ok(notificationService.listMine(page, limit, unreadOnly));
    }

    @Operation(summary = "获取未读通知数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount());
    }

    @Operation(summary = "标记通知已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.ok();
    }

    @Operation(summary = "标记所有通知已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.ok();
    }

    @Operation(summary = "删除单条通知")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationService.deleteById(id);
        return Result.ok();
    }

    @Operation(summary = "批量删除通知")
    @PostMapping("/delete-batch")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        notificationService.deleteBatch(ids);
        return Result.ok();
    }
}
