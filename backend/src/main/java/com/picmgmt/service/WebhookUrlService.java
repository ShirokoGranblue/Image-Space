package com.picmgmt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookUrlService {

    private final String publicBaseUrl;
    private final String webhookPath;

    public WebhookUrlService(
            @Value("${app.public-base-url:http://20.200.212.66}") String publicBaseUrl,
            @Value("${app.webhook-path:/api/webhook}") String webhookPath) {
        this.publicBaseUrl = publicBaseUrl;
        this.webhookPath = webhookPath;
    }

    public String getWebhookUrl() {
        String base = trimTrailingSlash(publicBaseUrl);
        String path = webhookPath == null || webhookPath.isBlank() ? "/" : webhookPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "/".equals(path) ? base + "/" : base + path;
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null || value.isBlank() ? "http://20.200.212.66" : value.trim();
        while (normalized.endsWith("/") && normalized.length() > "http://".length()) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
