package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.AppUser;
import com.anhvu.vlxd.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    public enum RequestStatus {
        ACCEPTED,
        MAIL_NOT_CONFIGURED,
        DELIVERY_FAILED
    }

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String mailUsername;
    private final String mailPassword;
    private final String mailFrom;
    private final int codeExpirationMinutes;
    private final int resendDelaySeconds;
    private final int maxAttempts;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(AppUserRepository appUserRepository,
                                PasswordEncoder passwordEncoder,
                                JavaMailSender mailSender,
                                @Value("${spring.mail.username:}") String mailUsername,
                                @Value("${spring.mail.password:}") String mailPassword,
                                @Value("${app.mail.from:}") String mailFrom,
                                @Value("${app.password-reset.code-expiration-minutes:10}") int codeExpirationMinutes,
                                @Value("${app.password-reset.resend-delay-seconds:60}") int resendDelaySeconds,
                                @Value("${app.password-reset.max-attempts:5}") int maxAttempts) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.mailFrom = mailFrom;
        this.codeExpirationMinutes = codeExpirationMinutes;
        this.resendDelaySeconds = resendDelaySeconds;
        this.maxAttempts = maxAttempts;
    }

    public RequestStatus requestResetCode(String email) {
        if (!mailConfigured()) {
            return RequestStatus.MAIL_NOT_CONFIGURED;
        }

        String normalizedEmail = normalizeEmail(email);
        Optional<AppUser> optionalUser = appUserRepository.findByEmailIgnoreCase(normalizedEmail);
        if (optionalUser.isEmpty()) {
            return RequestStatus.ACCEPTED;
        }

        AppUser user = optionalUser.get();
        LocalDateTime now = LocalDateTime.now();
        if (user.getResetRequestedAt() != null
                && user.getResetRequestedAt().isAfter(now.minusSeconds(resendDelaySeconds))) {
            return RequestStatus.ACCEPTED;
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(900_000) + 100_000);
        user.setResetToken(passwordEncoder.encode(code));
        user.setResetTokenExpiresAt(now.plusMinutes(codeExpirationMinutes));
        user.setResetRequestedAt(now);
        user.setResetAttempts(0);
        appUserRepository.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom == null || mailFrom.isBlank() ? mailUsername : mailFrom);
        message.setTo(normalizedEmail);
        message.setSubject("Mã xác nhận đặt lại mật khẩu Anh Vũ");
        message.setText("Mã xác nhận của bạn là: " + code + "\n\n"
                + "Mã có hiệu lực trong " + codeExpirationMinutes + " phút. "
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này.");

        try {
            mailSender.send(message);
            return RequestStatus.ACCEPTED;
        }
        catch (MailException exception) {
            clearResetState(user);
            appUserRepository.save(user);
            return RequestStatus.DELIVERY_FAILED;
        }
    }

    public Optional<String> verifyCode(String email, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return Optional.empty();
        }

        Optional<AppUser> optionalUser = appUserRepository.findByEmailIgnoreCase(normalizeEmail(email));
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        AppUser user = optionalUser.get();
        if (!codeStateValid(user)) {
            return Optional.empty();
        }

        if (!passwordEncoder.matches(code, user.getResetToken())) {
            int attempts = user.getResetAttempts() == null ? 1 : user.getResetAttempts() + 1;
            if (attempts >= maxAttempts) {
                clearResetState(user);
            }
            else {
                user.setResetAttempts(attempts);
            }
            appUserRepository.save(user);
            return Optional.empty();
        }

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(codeExpirationMinutes));
        user.setResetRequestedAt(null);
        user.setResetAttempts(0);
        appUserRepository.save(user);
        return Optional.of(resetToken);
    }

    public boolean isResetTokenValid(String token) {
        return findValidResetToken(token).isPresent();
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<AppUser> optionalUser = findValidResetToken(token);
        if (optionalUser.isEmpty()) {
            return false;
        }

        AppUser user = optionalUser.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setActive(true);
        clearResetState(user);
        appUserRepository.save(user);
        return true;
    }

    private Optional<AppUser> findValidResetToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return appUserRepository.findByResetToken(token)
                .filter(user -> user.getResetTokenExpiresAt() != null
                        && user.getResetTokenExpiresAt().isAfter(LocalDateTime.now()));
    }

    private boolean codeStateValid(AppUser user) {
        return user.getResetToken() != null
                && user.getResetToken().startsWith("$2")
                && user.getResetTokenExpiresAt() != null
                && user.getResetTokenExpiresAt().isAfter(LocalDateTime.now())
                && (user.getResetAttempts() == null || user.getResetAttempts() < maxAttempts);
    }

    private boolean mailConfigured() {
        return mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void clearResetState(AppUser user) {
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        user.setResetRequestedAt(null);
        user.setResetAttempts(0);
    }
}
