package com.picmgmt.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebhookUrlServiceTest {

    @Test
    void getWebhookUrl_shouldJoinBaseUrlAndWebhookPath() {
        WebhookUrlService service = new WebhookUrlService("https://image-space.app/", "api/webhook");

        assertEquals("https://image-space.app/api/webhook", service.getWebhookUrl());
    }

    @Test
    void getWebhookUrl_shouldKeepRootPathStable() {
        WebhookUrlService service = new WebhookUrlService("https://image-space.app", "/");

        assertEquals("https://image-space.app/", service.getWebhookUrl());
    }
}
