package vn.hoidanit.notificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vn.hoidanit.notificationservice.annotation.RateLimit;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @RateLimit(name = "sendEmail")
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendSimpleEmailFallback")
    @Retry(name = "emailService")
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Fallback for simple email sending
     */
    private void sendSimpleEmailFallback(String to, String subject, String text, Throwable ex) {
        log.error("Circuit breaker fallback triggered for simple email to {}: {}", to, ex.getMessage());
        log.debug("Exception type: {}", ex.getClass().getName());
        // In production, you might want to queue this email for later retry
        // or send notification to monitoring system
    }

    @RateLimit(name = "sendEmail")
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendHtmlEmailFallback")
    @Retry(name = "emailService")
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);

            // Process Thymeleaf template
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Fallback for HTML email sending
     */
    private void sendHtmlEmailFallback(String to, String subject, String templateName,
                                       Map<String, Object> variables, Throwable ex) {
        log.error("Circuit breaker fallback triggered for HTML email to {}: {}", to, ex.getMessage());
        log.debug("Exception type: {}", ex.getClass().getName());
        // In production, you might want to queue this email for later retry
        // or send notification to monitoring system
    }
}


