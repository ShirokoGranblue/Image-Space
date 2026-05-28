package com.picmgmt.service.impl;

import com.picmgmt.common.BusinessException;
import com.picmgmt.config.TurnstileProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TurnstileServiceImplTest {

    @Test
    void verifySkipsWhenDisabled() {
        TurnstileProperties properties = new TurnstileProperties();
        properties.setEnabled(false);
        TurnstileServiceImpl service = new TurnstileServiceImpl(properties, builderReturning(mock(RestTemplate.class)));

        assertDoesNotThrow(() -> service.verify(null, "127.0.0.1"));
    }

    @Test
    void verifyRejectsMissingTokenWhenEnabled() {
        TurnstileProperties properties = enabledProperties();
        TurnstileServiceImpl service = new TurnstileServiceImpl(properties, builderReturning(mock(RestTemplate.class)));

        assertThrows(BusinessException.class, () -> service.verify(" ", "127.0.0.1"));
    }

    @Test
    void verifyAcceptsSuccessfulCloudflareResponse() {
        TurnstileProperties properties = enabledProperties();
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(eq(properties.getSiteverifyUrl()), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("success", true)));
        TurnstileServiceImpl service = new TurnstileServiceImpl(properties, builderReturning(restTemplate));

        assertDoesNotThrow(() -> service.verify("token", "127.0.0.1"));
    }

    @Test
    void verifyRejectsFailedCloudflareResponse() {
        TurnstileProperties properties = enabledProperties();
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(eq(properties.getSiteverifyUrl()), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("success", false)));
        TurnstileServiceImpl service = new TurnstileServiceImpl(properties, builderReturning(restTemplate));

        assertThrows(BusinessException.class, () -> service.verify("token", "127.0.0.1"));
    }

    private TurnstileProperties enabledProperties() {
        TurnstileProperties properties = new TurnstileProperties();
        properties.setEnabled(true);
        properties.setSecretKey("secret");
        properties.setSiteverifyUrl("https://challenges.cloudflare.com/turnstile/v0/siteverify");
        return properties;
    }

    private RestTemplateBuilder builderReturning(RestTemplate rest) {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(rest);
        return builder;
    }
}
