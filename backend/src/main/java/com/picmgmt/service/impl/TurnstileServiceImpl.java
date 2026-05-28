package com.picmgmt.service.impl;

import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.config.TurnstileProperties;
import com.picmgmt.service.TurnstileService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.util.Map;

@Service
public class TurnstileServiceImpl implements TurnstileService {

    private final TurnstileProperties properties;
    private final RestOperations restOperations;

    public TurnstileServiceImpl(TurnstileProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restOperations = builder.build();
    }

    @Override
    public void verify(String token, String remoteIp) {
        if (!properties.isEnabled()) {
            return;
        }
        if (isBlank(properties.getSecretKey()) || isBlank(token)) {
            throw new BusinessException(ErrorCode.TURNSTILE_INVALID);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", properties.getSecretKey());
        form.add("response", token);
        if (!isBlank(remoteIp)) {
            form.add("remoteip", remoteIp);
        }

        try {
            ResponseEntity<Map> response = restOperations.postForEntity(
                    properties.getSiteverifyUrl(),
                    new HttpEntity<>(form, headers),
                    Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
                throw new BusinessException(ErrorCode.TURNSTILE_INVALID);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.TURNSTILE_INVALID, e.getMessage());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
