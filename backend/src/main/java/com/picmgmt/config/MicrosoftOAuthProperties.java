package com.picmgmt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Microsoft OAuth2 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth.microsoft")
public class MicrosoftOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tenant = "common";
    private String scope = "openid profile email User.Read";
    private String frontendSuccessUrl;
    private String frontendFailureUrl;
    private String authorizeUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    private String tokenUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private String userInfoUrl = "https://graph.microsoft.com/v1.0/me";
}
