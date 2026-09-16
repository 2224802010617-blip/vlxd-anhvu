package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.web.CustomerOrderForm;
import com.anhvu.vlxd.web.QuoteRequestForm;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("quoteForm")
    public QuoteRequestForm quoteForm() {
        return new QuoteRequestForm();
    }

    @ModelAttribute("orderForm")
    public CustomerOrderForm orderForm() {
        return new CustomerOrderForm();
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    /**
     * Ten hien tren thanh dau trang: uu tien ho ten that (tai khoan Google co san),
     * khong co thi lay phan truoc dau @ cua email. Tranh hien ky tu vo nghia nhu "2"
     * voi email bat dau bang so.
     */
    @ModelAttribute("currentDisplayName")
    public String currentDisplayName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return "";
        }
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            String name = oidcUser.getFullName();
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
        }
        String name = authentication.getName() == null ? "" : authentication.getName().trim();
        int at = name.indexOf('@');
        return at > 0 ? name.substring(0, at) : name;
    }
}
