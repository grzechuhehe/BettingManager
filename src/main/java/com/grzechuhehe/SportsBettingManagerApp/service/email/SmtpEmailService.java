package com.grzechuhehe.SportsBettingManagerApp.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@sportsbetting.com}")
    private String fromEmail;

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            Context context = new Context();
            context.setVariable("resetLink", resetLink);
            String htmlContent = templateEngine.process("password-reset", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Resetowanie hasła - SportsBettingManager");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Wysłano email z linkiem do resetu hasła na adres: {}", to);
        } catch (MessagingException e) {
            log.error("Błąd podczas wysyłania emaila do {}: {}", to, e.getMessage());
        }
    }
}
