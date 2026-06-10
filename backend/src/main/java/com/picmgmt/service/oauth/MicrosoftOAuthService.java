package com.picmgmt.service.oauth;

import com.picmgmt.dto.oauth.MicrosoftTokenResponse;
import com.picmgmt.dto.oauth.MicrosoftUserInfo;

/**
 * Microsoft OAuth2 服务接口
 */
public interface MicrosoftOAuthService {

    /**
     * 构建 Microsoft 授权 URL
     *
     * @param state CSRF 防护 state
     * @return 完整的授权 URL
     */
    String buildAuthorizeUrl(String state);

    /**
     * 使用授权码换取 token
     *
     * @param code 授权码
     * @return token 响应
     */
    MicrosoftTokenResponse exchangeCodeForToken(String code);

    /**
     * 获取 Microsoft 用户信息
     *
     * @param accessToken 访问令牌
     * @return 用户信息
     */
    MicrosoftUserInfo getMicrosoftUserInfo(String accessToken);
}
