package com.picmgmt.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.config.TencentSesConfig;
import com.picmgmt.service.EmailService;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.ses.v20201002.SesClient;
import com.tencentcloudapi.ses.v20201002.models.SendEmailRequest;
import com.tencentcloudapi.ses.v20201002.models.SendEmailResponse;
import com.tencentcloudapi.ses.v20201002.models.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "tencent-ses")
public class TencentSesEmailService implements EmailService {

    private static final long VERIFICATION_TRIGGER_TYPE = 1L;

    private final SesClient sesClient;
    private final TencentSesConfig config;
    private final ObjectMapper objectMapper;

    public TencentSesEmailService(SesClient sesClient,
                                  TencentSesConfig config,
                                  ObjectMapper objectMapper) {
        this.sesClient = sesClient;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        String maskedRecipient = EmailAddressMasker.mask(to);
        try {
            Template template = new Template();
            template.setTemplateID(config.getTemplateId());
            template.setTemplateData(objectMapper.writeValueAsString(Map.of("code", code)));

            SendEmailRequest request = new SendEmailRequest();
            request.setFromEmailAddress(config.getFromAddress());
            request.setSubject(config.getSubject());
            request.setDestination(new String[]{to});
            request.setTemplate(template);
            request.setTriggerType(VERIFICATION_TRIGGER_TYPE);

            SendEmailResponse response = sesClient.SendEmail(request);
            log.info("Tencent SES verification email accepted for {}; messageId={}; requestId={}",
                    maskedRecipient, response.getMessageId(), response.getRequestId());
        } catch (TencentCloudSDKException e) {
            log.error("Tencent SES verification email failed for {}; errorCode={}; requestId={}",
                    maskedRecipient, e.getErrorCode(), e.getRequestId());
            throw new RuntimeException("邮件发送失败", e);
        } catch (JsonProcessingException e) {
            log.error("Tencent SES verification email could not be prepared for {}; errorType={}",
                    maskedRecipient, e.getClass().getSimpleName());
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
