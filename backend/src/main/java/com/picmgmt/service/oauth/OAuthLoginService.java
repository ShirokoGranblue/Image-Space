package com.picmgmt.service.oauth;

import com.picmgmt.dto.oauth.MicrosoftUserInfo;
import com.picmgmt.dto.oauth.OAuthLoginResult;

/**
 * OAuth 登录服务接口
 */
public interface OAuthLoginService {

    /**
     * Microsoft 登录或注册
     *
     * @param userInfo Microsoft 用户信息
     * @return 登录结果
     */
    OAuthLoginResult loginOrRegisterByMicrosoft(MicrosoftUserInfo userInfo);
}
