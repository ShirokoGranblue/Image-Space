package com.picmgmt.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebhookUrlServiceTest {

    @Test
    void getWebhookUrl_shouldJoinBaseUrlAndWebhookPath() {
        WebhookUrlService service = new WebhookUrlService("http://4.230.10.11/", "api/webhook");

        assertEquals("http://4.230.10.11/api/webhook", service.getWebhookUrl());
    }

    @Test
    void getWebhookUrl_shouldKeepRootPathStable() {
        WebhookUrlService service = new WebhookUrlService("http://4.230.10.11", "/");

        assertEquals("http://4.230.10.11/", service.getWebhookUrl());
    }
}
