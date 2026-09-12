package com.anhvu.vlxd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cong gui mail duy nhat cua he thong.
 * Railway chan moi cong SMTP (25/465/587) nen uu tien gui qua HTTPS API cua Brevo khi co BREVO_API_KEY;
 * khong co key thi dung SMTP (chay duoc tren may local / host khac).
 */
@Service
public class MailGateway {

    private static final Logger log = LoggerFactory.getLogger(MailGateway.class);
    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";
    private static final String RESEND_ENDPOINT = "https://api.resend.com/emails";
    /** Dia chi gui san cua Resend, dung duoc ngay khong can xac minh ten mien. */
    private static final String RESEND_DEFAULT_FROM = "onboarding@resend.dev";

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String brevoApiKey;
    private final String resendApiKey;
    private final String resendFrom;
    private final String mailUsername;
    private final String mailPassword;
    private final String mailFrom;
    private final String senderName;

    public MailGateway(JavaMailSender mailSender,
                       ObjectMapper objectMapper,
                       @Value("${app.mail.brevo-api-key:}") String brevoApiKey,
                       @Value("${app.mail.resend-api-key:}") String resendApiKey,
                       @Value("${app.mail.resend-from:}") String resendFrom,
                       @Value("${spring.mail.username:}") String mailUsername,
                       @Value("${spring.mail.password:}") String mailPassword,
                       @Value("${app.mail.from:}") String mailFrom,
                       @Value("${app.mail.sender-name:VLXD Anh Vu}") String senderName) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.resendApiKey = resendApiKey == null ? "" : resendApiKey.trim();
        this.resendFrom = resendFrom == null || resendFrom.isBlank() ? RESEND_DEFAULT_FROM : resendFrom.trim();
        this.mailUsername = mailUsername == null ? "" : mailUsername.trim();
        this.mailPassword = mailPassword == null ? "" : mailPassword.trim();
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
        this.senderName = senderName;
    }

    public boolean isConfigured() {
        return !brevoApiKey.isEmpty() || !resendApiKey.isEmpty()
                || (!mailUsername.isEmpty() && !mailPassword.isEmpty());
    }

    public String channel() {
        if (!brevoApiKey.isEmpty()) {
            return "Brevo API";
        }
        if (!resendApiKey.isEmpty()) {
            return "Resend API";
        }
        return isConfigured() ? "SMTP " + mailUsername : "chưa cấu hình";
    }

    /** Gui mail van ban. Nem ra IllegalStateException kem ly do neu gui that bai. */
    public void send(String to, String subject, String text) {
        if (!brevoApiKey.isEmpty()) {
            sendViaBrevo(to, subject, text);
        } else if (!resendApiKey.isEmpty()) {
            sendViaResend(to, subject, text);
        } else {
            sendViaSmtp(to, subject, text);
        }
    }

    private String fromAddress() {
        return mailFrom.isEmpty() ? mailUsername : mailFrom;
    }

    private void sendViaBrevo(String to, String subject, String text) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sender", Map.of("name", senderName, "email", fromAddress()));
            body.put("to", List.of(Map.of("email", to)));
            body.put("subject", subject);
            body.put("textContent", text);
            HttpRequest request = HttpRequest.newBuilder(URI.create(BREVO_ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", brevoApiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Brevo trả về HTTP " + response.statusCode() + ": " + response.body());
            }
            log.info("Da gui mail qua Brevo toi {}: {}", to, subject);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Không gọi được Brevo API: " + e.getMessage(), e);
        }
    }

    private void sendViaResend(String to, String subject, String text) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", resendFrom.contains("<") ? resendFrom : senderName + " <" + resendFrom + ">");
            body.put("to", List.of(to));
            body.put("subject", subject);
            body.put("text", text);
            HttpRequest request = HttpRequest.newBuilder(URI.create(RESEND_ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Resend trả về HTTP " + response.statusCode() + ": " + response.body());
            }
            log.info("Da gui mail qua Resend toi {}: {}", to, subject);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Không gọi được Resend API: " + e.getMessage(), e);
        }
    }

    private void sendViaSmtp(String to, String subject, String text) {
        if (mailUsername.isEmpty() || mailPassword.isEmpty()) {
            throw new IllegalStateException("Chưa cấu hình MAIL_USERNAME / MAIL_PASSWORD hoặc BREVO_API_KEY.");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String from = fromAddress();
            message.setFrom(from.contains("<") ? from : senderName + " <" + from + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Da gui mail qua SMTP toi {}: {}", to, subject);
        } catch (Exception e) {
            throw new IllegalStateException("SMTP lỗi: " + e.getMessage(), e);
        }
    }
}
