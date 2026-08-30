package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.QuoteRequest;
import com.anhvu.vlxd.repository.QuoteRequestRepository;
import com.anhvu.vlxd.web.QuoteRequestForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class QuoteRequestController {

    private static final String COMPANY_NAME = "C\u00D4NG TY TNHH MTV TM DV XD ANH V\u0168";
    private static final String BANK_ACCOUNT = "1991777790";
    private static final String BANK_NAME = "Ng\u00E2n h\u00E0ng Qu\u00E2n \u0111\u1ED9i - Chi nh\u00E1nh B\u00ECnh Long";
    private static final String EMAIL = "xaydungvantaidaphuongthucanhvu@gmail.com";

    private final QuoteRequestRepository quoteRequestRepository;

    @PostMapping("/quote-request")
    public String store(@Valid @ModelAttribute("quoteForm") QuoteRequestForm form,
                        BindingResult bindingResult,
                        Model model) {
        if (bindingResult.hasErrors()) {
            addCommonAttributes(model);
            model.addAttribute("quoteForm", form);
            model.addAttribute("quoteSuccess", false);
            return "quote";
        }

        quoteRequestRepository.save(QuoteRequest.builder()
                .customerName(form.getCustomerName().trim())
                .phone(form.getPhone().trim())
                .address(form.getAddress() == null ? "" : form.getAddress().trim())
                .content(form.getContent().trim())
                .status("NEW")
                .build());

        return "redirect:/bao-gia?quoteSuccess=true#quote-form";
    }

    @GetMapping("/bao-gia")
    public String quotePage(@RequestParam(defaultValue = "false") boolean quoteSuccess,
                            @RequestParam(required = false) BigDecimal calculatedQuantity,
                            @RequestParam(required = false) BigDecimal areaM2,
                            @RequestParam(required = false) BigDecimal consumptionPerM2,
                            Model model) {
        addCommonAttributes(model);
        model.addAttribute("quoteSuccess", quoteSuccess);
        model.addAttribute("calculatedQuantity", calculatedQuantity);
        model.addAttribute("areaM2", areaM2);
        model.addAttribute("consumptionPerM2", consumptionPerM2);
        return "quote";
    }

    private void addCommonAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        String currentEmail = "";
        if (authenticated && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            currentEmail = oidcUser.getEmail();
        } else if (authenticated) {
            currentEmail = authentication.getName();
        }

        model.addAttribute("companyName", COMPANY_NAME);
        model.addAttribute("bankAccount", BANK_ACCOUNT);
        model.addAttribute("bankName", BANK_NAME);
        model.addAttribute("email", EMAIL);
        model.addAttribute("authenticated", authenticated);
        model.addAttribute("currentEmail", currentEmail);
    }
}
