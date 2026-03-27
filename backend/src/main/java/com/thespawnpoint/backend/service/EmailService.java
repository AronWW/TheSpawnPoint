package com.thespawnpoint.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendVerificationCode(String toEmail, String code) {
        sendVerificationCode(toEmail, code, "Гравець");
    }

    public void sendVerificationCode(String toEmail, String code, String displayName) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("displayName", displayName);

        String html = templateEngine.process("email/verification", context);
        sendHtmlEmail(toEmail, "Підтвердження пошти — TheSpawnPoint", html);
    }

    public void sendPasswordResetLink(String toEmail, String token) {
        sendPasswordResetLink(toEmail, token, "Гравець");
    }

    public void sendPasswordResetLink(String toEmail, String token, String displayName) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        Context context = new Context();
        context.setVariable("resetLink", resetLink);
        context.setVariable("displayName", displayName);

        String html = templateEngine.process("email/reset-password", context);
        sendHtmlEmail(toEmail, "Скидання пароля — TheSpawnPoint", html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}