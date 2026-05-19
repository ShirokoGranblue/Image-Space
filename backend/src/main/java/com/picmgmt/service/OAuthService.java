package com.picmgmt.service;

public interface OAuthService {

    String getAuthorizeUrl(String provider);

    String handleCallback(String provider, String code, String state);
}
