package com.picmgmt.service;

public interface TurnstileService {

    void verify(String token, String remoteIp);
}
