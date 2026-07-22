package com.picmgmt.service.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class SmtpEmailServiceTest {

    @Test
    void sendsAstralSpaceCodeWithFiveMinuteValidityAndMaskedLogs(CapturedOutput output) throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        SmtpEmailService service = new SmtpEmailService(mailSender, "sender@example.com");

        service.sendVerificationCode("user@example.com", "123456");

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("AstralSpace 验证码");
        assertThat(message.getContent().toString())
                .contains("AstralSpace 验证码")
                .contains("123456")
                .contains("5 分钟内有效")
                .doesNotContain("60秒内有效");
        assertThat(output.getOut())
                .contains("u***r@example.com")
                .doesNotContain("user@example.com")
                .doesNotContain("123456");
    }
}
