package com.picmgmt.service.impl;

import com.picmgmt.service.SmsService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${sms.tencent.secret-id}")
    private String secretId;

    @Value("${sms.tencent.secret-key}")
    private String secretKey;

    @Value("${sms.tencent.sdk-app-id}")
    private String sdkAppId;

    @Value("${sms.tencent.sign-name}")
    private String signName;

    @Value("${sms.tencent.template-id}")
    private String templateId;

    @Override
    public void sendVerificationCode(String phone, String code) {
        try {
            Credential cred = new Credential(secretId, secretKey);
            SmsClient client = new SmsClient(cred, "ap-guangzhou");
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(sdkAppId);
            req.setSignName(signName);
            req.setTemplateId(templateId);
            req.setTemplateParamSet(new String[]{code});
            req.setPhoneNumberSet(new String[]{"+86" + phone});
            client.SendSms(req);
            log.info("SMS code sent to {}", phone);
        } catch (TencentCloudSDKException e) {
            log.error("Failed to send SMS to {}", phone, e);
            throw new RuntimeException("短信发送失败", e);
        }
    }
}
