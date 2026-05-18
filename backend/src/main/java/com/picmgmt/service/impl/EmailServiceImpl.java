package com.picmgmt.service.impl;

import com.picmgmt.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("ImageSpace 登录验证码");
            helper.setText(String.format("""
                    <div style="font-family:sans-serif;max-width:480px;margin:0 auto;">
                        <h2 style="color:#2563eb;">ImageSpace 登录验证码</h2>
                        <p>您的验证码是：</p>
                        <div style="font-size:32px;font-weight:bold;color:#2563eb;
                                    padding:16px 24px;background:#eff4ff;border-radius:8px;
                                    text-align:center;letter-spacing:6px;">%s</div>
                        <p style="color:#6b7a8d;margin-top:16px;">验证码60秒内有效，请勿泄露给他人。</p>
                    </div>
                    """, code), true);
            mailSender.send(message);
            log.info("Verification code sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification code to {}", to, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
