package com.picmgmt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "media.internal")
public class InternalMediaProperties {

    private String token;
}
