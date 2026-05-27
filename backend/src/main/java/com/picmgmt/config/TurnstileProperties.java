package com.picmgmt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "turnstile")
public class TurnstileProperties {

    private boolean enabled = false;

    private String secretKey;

    private String siteverifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
}
