package com.picmgmt.controller;

import com.picmgmt.common.Result;
import com.picmgmt.service.WebhookUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final WebhookUrlService webhookUrlService;

    @Operation(summary = "获取 Webhook URL")
    @GetMapping("/webhook-url")
    public Result<Map<String, String>> webhookUrl() {
        return Result.ok(Map.of("webhookUrl", webhookUrlService.getWebhookUrl()));
    }
}
