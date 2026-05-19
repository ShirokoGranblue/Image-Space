package com.picmgmt.service.impl;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${sms.aliyun.access-key-id}")
    private String accessKeyId;

    @Value("${sms.aliyun.access-key-secret}")
    private String accessKeySecret;

    @Value("${sms.aliyun.sign-name}")
    private String signName;

    @Value("${sms.aliyun.template-code}")
    private String templateCode;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void sendVerificationCode(String phone, String code) {
        try {
            DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
            IAcsClient client = new DefaultAcsClient(profile);
            CommonRequest request = new CommonRequest();
            request.setSysMethod(MethodType.POST);
            request.setSysDomain("dysmsapi.aliyuncs.com");
            request.setSysVersion("2017-05-25");
            request.setSysAction("SendSms");
            request.putQueryParameter("PhoneNumbers", phone);
            request.putQueryParameter("SignName", signName);
            request.putQueryParameter("TemplateCode", templateCode);
            request.putQueryParameter("TemplateParam", "{\"code\":\"" + code + "\"}");
            CommonResponse response = client.getCommonResponse(request);
            String data = response.getData();
            log.info("SMS sent to {}, response: {}", phone, data);
            JsonNode json = objectMapper.readTree(data);
            String respCode = json.has("Code") ? json.get("Code").asText() : "";
            if (!"OK".equals(respCode)) {
                String msg = json.has("Message") ? json.get("Message").asText() : respCode;
                throw new RuntimeException("阿里云短信错误: " + msg);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to {}", phone, e);
            throw new RuntimeException("短信发送失败", e);
        }
    }
}
