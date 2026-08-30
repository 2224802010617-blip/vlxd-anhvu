package com.anhvu.vlxd.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminAccessPolicy {

    private final String adminEmail;

    public AdminAccessPolicy(@Value("${app.security.admin-email}") String adminEmail) {
        this.adminEmail = normalize(adminEmail);
    }

    public boolean isAdmin(String email) {
        return !adminEmail.isBlank() && adminEmail.equals(normalize(email));
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
