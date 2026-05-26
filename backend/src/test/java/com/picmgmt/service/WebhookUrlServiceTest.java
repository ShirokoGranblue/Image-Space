package com.picmgmt.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebhookUrlServiceTest {

    @Test
    void getWebhookUrl_shouldJoinBaseUrlAndWebhookPath() {
        WebhookUrlService service = new WebhookUrlService("http://20.200.212.66/", "api/webhook");

        assertEquals("http://20.200.212.66/api/webhook", service.getWebhookUrl());
    }

    @Test
    void getWebhookUrl_shouldKeepRootPathStable() {
        WebhookUrlService service = new WebhookUrlService("http://20.200.212.66", "/");

        assertEquals("http://20.200.212.66/", service.getWebhookUrl());
    }
}
