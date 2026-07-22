package com.picmgmt.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.config.TencentSesConfig;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.ses.v20201002.SesClient;
import com.tencentcloudapi.ses.v20201002.models.SendEmailRequest;
import com.tencentcloudapi.ses.v20201002.models.SendEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class TencentSesEmailServiceTest {

    private SesClient sesClient;
    private TencentSesEmailService service;

    @BeforeEach
    void setUp() {
        sesClient = mock(SesClient.class);
        TencentSesConfig config = new TencentSesConfig();
        config.setFromAddress("AstralSpace <no-reply@mail.image-space.app>");
        config.setSubject("AstralSpace 验证码");
        config.setTemplateId(100091L);
        service = new TencentSesEmailService(sesClient, config, new ObjectMapper());
    }

    @Test
    void sendsTriggeredTemplateEmailAndLogsOnlyProviderIds(CapturedOutput output) throws Exception {
        SendEmailResponse response = new SendEmailResponse();
        response.setMessageId("message-id");
        response.setRequestId("request-id");
        when(sesClient.SendEmail(any())).thenReturn(response);

        service.sendVerificationCode("user@example.com", "123456");

        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).SendEmail(requestCaptor.capture());
        SendEmailRequest request = requestCaptor.getValue();
        assertThat(request.getFromEmailAddress()).isEqualTo("AstralSpace <no-reply@mail.image-space.app>");
        assertThat(request.getSubject()).isEqualTo("AstralSpace 验证码");
        assertThat(request.getDestination()).containsExactly("user@example.com");
        assertThat(request.getTriggerType()).isEqualTo(1L);
        assertThat(request.getTemplate().getTemplateID()).isEqualTo(100091L);
        assertThat(request.getTemplate().getTemplateData()).isEqualTo("{\"code\":\"123456\"}");
        assertThat(output.getOut())
                .contains("u***r@example.com")
                .contains("message-id")
                .contains("request-id")
                .doesNotContain("user@example.com")
                .doesNotContain("123456");
    }

    @Test
    void wrapsProviderFailureWithoutLeakingRecipientOrCode(CapturedOutput output) throws Exception {
        when(sesClient.SendEmail(any()))
                .thenThrow(new TencentCloudSDKException(
                        "provider rejected request", "request-id", "InvalidParameter"));

        assertThatThrownBy(() -> service.sendVerificationCode("user@example.com", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("邮件发送失败");

        assertThat(output.getOut())
                .contains("u***r@example.com")
                .contains("InvalidParameter")
                .contains("request-id")
                .doesNotContain("user@example.com")
                .doesNotContain("123456");
    }
}
