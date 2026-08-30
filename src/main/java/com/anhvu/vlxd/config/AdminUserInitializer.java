package com.anhvu.vlxd.config;

import com.anhvu.vlxd.entity.AppUser;
import com.anhvu.vlxd.repository.AppUserRepository;
import com.anhvu.vlxd.security.AdminAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAccessPolicy adminAccessPolicy;

    @Value("${app.security.admin-password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        String adminEmail = adminAccessPolicy.getAdminEmail();
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }

        appUserRepository.findAllByRoleIgnoreCase("ADMIN").stream()
                .filter(user -> !adminAccessPolicy.isAdmin(user.getEmail()))
                .forEach(user -> {
                    user.setRole("USER");
                    appUserRepository.save(user);
                });

        appUserRepository.findByEmailIgnoreCase(adminEmail).ifPresentOrElse(user -> {
            user.setRole("ADMIN");
            user.setActive(true);
            if (hasConfiguredAdminPassword()) {
                user.setPassword(passwordEncoder.encode(adminPassword));
            }
            appUserRepository.save(user);
        }, () -> appUserRepository.save(AppUser.builder()
                .username(adminEmail)
                .email(adminEmail)
                .fullName("Quan tri Anh Vu")
                .password(passwordEncoder.encode(initialAdminPassword()))
                .role("ADMIN")
                .active(true)
                .build()));
    }

    private boolean hasConfiguredAdminPassword() {
        return adminPassword != null && !adminPassword.isBlank();
    }

    private String initialAdminPassword() {
        return hasConfiguredAdminPassword() ? adminPassword : UUID.randomUUID().toString();
    }
}
