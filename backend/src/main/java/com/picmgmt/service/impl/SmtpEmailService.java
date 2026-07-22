package com.picmgmt.service.impl;

import com.picmgmt.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        String maskedRecipient = EmailAddressMasker.mask(to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setFrom(fromAddress);
            helper.setSubject("AstralSpace 验证码");
            helper.setText(String.format("""
                    <div style="font-family:Arial,'PingFang SC','Microsoft YaHei',sans-serif;max-width:480px;margin:0 auto;color:#20282b;">
                        <p style="font-size:13px;font-weight:700;letter-spacing:.12em;color:#bf4935;margin:0 0 20px;">ASTRALSPACE</p>
                        <h2 style="font-size:26px;margin:0 0 20px;">AstralSpace 验证码</h2>
                        <p>使用下面的验证码继续完成操作：</p>
                        <div style="font-size:32px;font-weight:700;color:#bf4935;padding:18px 24px;background:#f3eee5;text-align:center;letter-spacing:6px;">%s</div>
                        <p style="color:#667175;margin-top:18px;">验证码 5 分钟内有效，请勿泄露给他人。</p>
                    </div>
                    """, code), true);
            mailSender.send(message);
            log.info("SMTP verification email accepted for {}", maskedRecipient);
        } catch (Exception e) {
            log.error("SMTP verification email failed for {}; errorType={}",
                    maskedRecipient, e.getClass().getSimpleName());
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
