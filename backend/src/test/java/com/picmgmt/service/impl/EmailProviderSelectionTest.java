package com.picmgmt.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picmgmt.config.TencentSesConfig;
import com.picmgmt.service.EmailService;
import com.tencentcloudapi.ses.v20201002.SesClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EmailProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(EmailConfiguration.class)
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withPropertyValues("spring.mail.username=sender@example.com");

    @Test
    void defaultsToSmtpWithoutRequiringTencentCredentials() {
        contextRunner.run(context -> {
            assertThat(context.getStartupFailure()).isNull();
            assertThat(context.getBeansOfType(EmailService.class)).hasSize(1);
            assertThat(context.getBean(EmailService.class)).isInstanceOf(SmtpEmailService.class);
            assertThat(context.getBeansOfType(SesClient.class)).isEmpty();
        });
    }

    @Test
    void selectsTencentSesWhenAllRequiredSettingsAreValid() {
        contextRunner
                .withPropertyValues(
                        "app.mail.provider=tencent-ses",
                        "app.mail.tencent-ses.secret-id=test-secret-id",
                        "app.mail.tencent-ses.secret-key=test-secret-key",
                        "app.mail.tencent-ses.region=ap-guangzhou",
                        "app.mail.tencent-ses.from-address=AstralSpace <no-reply@mail.image-space.app>",
                        "app.mail.tencent-ses.template-id=100091"
                )
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                    assertThat(context.getBeansOfType(EmailService.class)).hasSize(1);
                    assertThat(context.getBean(EmailService.class)).isInstanceOf(TencentSesEmailService.class);
                    assertThat(context.getBeansOfType(SesClient.class)).hasSize(1);
                });
    }

    @Test
    void rejectsUnsupportedTencentSesRegionAtStartup() {
        contextRunner
                .withPropertyValues(
                        "app.mail.provider=tencent-ses",
                        "app.mail.tencent-ses.secret-id=test-secret-id",
                        "app.mail.tencent-ses.secret-key=test-secret-key",
                        "app.mail.tencent-ses.region=ap-shanghai",
                        "app.mail.tencent-ses.from-address=AstralSpace <no-reply@mail.image-space.app>",
                        "app.mail.tencent-ses.template-id=100091"
                )
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Configuration(proxyBeanMethods = false)
    @Import({TencentSesConfig.class, SmtpEmailService.class, TencentSesEmailService.class})
    static class EmailConfiguration {}
}
