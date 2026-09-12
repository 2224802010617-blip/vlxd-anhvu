package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.AppUser;
import com.anhvu.vlxd.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private MailGateway mailGateway;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = newService();
    }

    @Test
    void sendsHashedCodeAndAcceptsTheCodeFromEmail() {
        AppUser user = user("customer@example.com");
        when(mailGateway.isConfigured()).thenReturn(true);
        when(appUserRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        PasswordResetService.RequestStatus status = passwordResetService.requestResetCode(user.getEmail());

        assertThat(status).isEqualTo(PasswordResetService.RequestStatus.ACCEPTED);
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailGateway).send(toCaptor.capture(), anyString(), bodyCaptor.capture());
        assertThat(toCaptor.getValue()).isEqualTo(user.getEmail());
        String code = extractCode(bodyCaptor.getValue());
        assertThat(user.getResetToken()).isNotEqualTo(code).startsWith("$2");
        assertThat(passwordEncoder.matches(code, user.getResetToken())).isTrue();

        Optional<String> resetToken = passwordResetService.verifyCode(user.getEmail(), code);

        assertThat(resetToken).isPresent();
        assertThat(user.getResetToken()).isEqualTo(resetToken.orElseThrow());
        when(appUserRepository.findByResetToken(resetToken.orElseThrow())).thenReturn(Optional.of(user));
        assertThat(passwordResetService.isResetTokenValid(resetToken.orElseThrow())).isTrue();
    }

    @Test
    void clearsResetStateWhenMailDeliveryFails() {
        AppUser user = user("customer@example.com");
        when(mailGateway.isConfigured()).thenReturn(true);
        when(appUserRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("Brevo tra ve HTTP 401"))
                .when(mailGateway).send(anyString(), anyString(), anyString());

        PasswordResetService.RequestStatus status = passwordResetService.requestResetCode(user.getEmail());

        assertThat(status).isEqualTo(PasswordResetService.RequestStatus.DELIVERY_FAILED);
        assertThat(user.getResetToken()).isNull();
        assertThat(user.getResetTokenExpiresAt()).isNull();
    }

    @Test
    void invalidatesCodeAfterFifthIncorrectAttempt() {
        AppUser user = user("customer@example.com");
        user.setResetToken(passwordEncoder.encode("123456"));
        user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setResetAttempts(4);
        when(appUserRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        Optional<String> resetToken = passwordResetService.verifyCode(user.getEmail(), "999999");

        assertThat(resetToken).isEmpty();
        assertThat(user.getResetToken()).isNull();
        assertThat(user.getResetTokenExpiresAt()).isNull();
    }

    @Test
    void doesNotQueryAccountsWhenMailIsNotConfigured() {
        when(mailGateway.isConfigured()).thenReturn(false);

        PasswordResetService.RequestStatus status = passwordResetService.requestResetCode("customer@example.com");

        assertThat(status).isEqualTo(PasswordResetService.RequestStatus.MAIL_NOT_CONFIGURED);
        verify(appUserRepository, never()).findByEmailIgnoreCase("customer@example.com");
    }

    private PasswordResetService newService() {
        return new PasswordResetService(
                appUserRepository,
                passwordEncoder,
                mailGateway,
                "sender@gmail.com",
                "app-password",
                "sender@gmail.com",
                10,
                60,
                5
        );
    }

    private AppUser user(String email) {
        return AppUser.builder()
                .id(1L)
                .username(email)
                .email(email)
                .password("encoded-password")
                .fullName("Customer")
                .role("USER")
                .active(true)
                .build();
    }

    private String extractCode(String body) {
        Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(body == null ? "" : body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
