package com.picmgmt.dto.oauth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OAuth 登录结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginResult {

    private Long userId;
    private String tokenName;
    private String tokenValue;
    private Boolean newlyCreated;
}
