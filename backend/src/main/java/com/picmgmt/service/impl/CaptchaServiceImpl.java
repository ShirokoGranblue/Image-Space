package com.picmgmt.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.picmgmt.cache.RedisCacheService;
import com.picmgmt.common.BusinessException;
import com.picmgmt.common.ErrorCode;
import com.picmgmt.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisCacheService redisCacheService;

    @Override
    public Map<String, String> getCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 80, 4, 150);
        captcha.createCode();
        String captchaId = UUID.randomUUID().toString();
        String code = captcha.getCode();
        redisCacheService.put("captcha:" + captchaId, code, Duration.ofSeconds(60));
        return Map.of(
            "captchaId", captchaId,
            "captchaImage", captcha.getImageBase64Data()
        );
    }

    @Override
    public void verify(String captchaId, String code) {
        String key = "captcha:" + captchaId;
        String storedCode = redisCacheService.get(key, String.class).orElse(null);
        if (storedCode == null || !storedCode.equalsIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        redisCacheService.evict(key);
    }
}
