package com.picmgmt.service;

public interface OAuthService {

    String getAuthorizeUrl(String provider, String baseUrl);

    OAuthResult handleCallback(String provider, String code, String state, String baseUrl);

    record OAuthResult(String token, String baseUrl) {}
}
