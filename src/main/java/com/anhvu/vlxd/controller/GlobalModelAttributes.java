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
}
