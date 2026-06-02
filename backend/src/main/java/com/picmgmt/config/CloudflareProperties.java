package com.picmgmt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cloudflare")
public class CloudflareProperties {

    private String zoneId;
    private String apiToken;
    private String purgeUrl = "https://api.cloudflare.com/client/v4/zones/%s/purge_cache";
}
