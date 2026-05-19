package com.picmgmt.service.impl;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.cache.RedisCacheService;
import com.picmgmt.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final RedisCacheService redisCacheService;

    @Value("${sms.aliyun.access-key-id}")
    private String accessKeyId;

    @Value("${sms.aliyun.access-key-secret}")
    private String accessKeySecret;

    @Value("${sms.aliyun.sign-name}")
    private String signName;

    @Value("${sms.aliyun.template-code}")
    private String templateCode;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private IAcsClient getClient() {
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        return new DefaultAcsClient(profile);
    }

    @Override
    public void sendVerificationCode(String phone) {
        try {
            IAcsClient client = getClient();
            CommonRequest request = new CommonRequest();
            request.setSysMethod(MethodType.POST);
            request.setSysDomain("dypnsapi.aliyuncs.com");
            request.setSysVersion("2017-05-25");
            request.setSysAction("SendSmsVerifyCode");
            request.putQueryParameter("PhoneNumber", phone);
            request.putQueryParameter("SignName", signName);
            request.putQueryParameter("TemplateCode", templateCode);
            request.putQueryParameter("TemplateParam", "{\"code\":\"##code##\"}");
            request.putQueryParameter("CodeType", "1");
            request.putQueryParameter("CodeLength", "4");
            request.putQueryParameter("ReturnVerifyCode", "true");
            log.info("Sending PNVS SMS to {} sign={} template={}", phone, signName, templateCode);
            CommonResponse response = client.getCommonResponse(request);
            String data = response.getData();
            log.info("PNVS SMS response for {}: {}", phone, data);
            JsonNode json = objectMapper.readTree(data);
            String respCode = json.has("Code") ? json.get("Code").asText() : "";
            if (!"OK".equals(respCode)) {
                String msg = json.has("Message") ? json.get("Message").asText() : respCode;
                throw new RuntimeException("阿里云PNVS错误: " + msg);
            }
            // Store PNVS-generated code in Redis for verification
            String verifyCode = json.at("/Model/VerifyCode").asText("");
            if (!verifyCode.isEmpty()) {
                redisCacheService.put("code:login:" + phone, verifyCode, Duration.ofSeconds(300));
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to {}", phone, e);
            throw new RuntimeException("短信发送失败", e);
        }
    }

    @Override
    public boolean checkVerificationCode(String phone, String code) {
        try {
            IAcsClient client = getClient();
            CommonRequest request = new CommonRequest();
            request.setSysMethod(MethodType.POST);
            request.setSysDomain("dypnsapi.aliyuncs.com");
            request.setSysVersion("2017-05-25");
            request.setSysAction("CheckSmsVerifyCode");
            request.putQueryParameter("PhoneNumber", phone);
            request.putQueryParameter("VerifyCode", code);
            log.info("Checking PNVS code for {}", phone);
            CommonResponse response = client.getCommonResponse(request);
            String data = response.getData();
            log.info("PNVS check response for {}: {}", phone, data);
            JsonNode json = objectMapper.readTree(data);
            String respCode = json.has("Code") ? json.get("Code").asText() : "";
            if (!"OK".equals(respCode)) {
                String msg = json.has("Message") ? json.get("Message").asText() : respCode;
                throw new RuntimeException("阿里云PNVS错误: " + msg);
            }
            return json.at("/Model/VerifyResult").asBoolean(false);
        } catch (Exception e) {
            log.error("Failed to check SMS code for {}", phone, e);
            throw new RuntimeException("验证码校验失败", e);
        }
    }
}
