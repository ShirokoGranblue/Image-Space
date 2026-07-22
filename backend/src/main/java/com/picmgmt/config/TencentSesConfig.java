package com.picmgmt.config;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.ses.v20201002.SesClient;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Getter
@Setter
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "app.mail.tencent-ses")
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "tencent-ses")
public class TencentSesConfig {

    private static final Set<String> SUPPORTED_REGIONS = Set.of("ap-guangzhou", "ap-hongkong");

    private String secretId;

    private String secretKey;

    private String region;

    private String fromAddress;

    private String subject = "AstralSpace 验证码";

    private Long templateId;

    @PostConstruct
    void validateConfiguration() {
        requireText(secretId, "app.mail.tencent-ses.secret-id");
        requireText(secretKey, "app.mail.tencent-ses.secret-key");
        requireText(fromAddress, "app.mail.tencent-ses.from-address");
        requireText(subject, "app.mail.tencent-ses.subject");
        if (!SUPPORTED_REGIONS.contains(region)) {
            throw new IllegalStateException(
                    "app.mail.tencent-ses.region must be ap-guangzhou or ap-hongkong");
        }
        if (templateId == null || templateId <= 0) {
            throw new IllegalStateException("app.mail.tencent-ses.template-id must be a positive integer");
        }
    }

    @Bean
    public SesClient sesClient() {
        return new SesClient(new Credential(secretId, secretKey), region);
    }

    private static void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must not be blank");
        }
    }
}
