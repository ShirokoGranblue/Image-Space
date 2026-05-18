package com.picmgmt.service;

import java.util.Map;

public interface CaptchaService {

    Map<String, String> getCaptcha();

    void verify(String captchaId, String code);
}
