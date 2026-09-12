package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.EmailContact;
import com.anhvu.vlxd.service.EmailContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/** Link huy nhan email trong cac tin gui hang loat. Khong yeu cau dang nhap. */
@Controller
@RequiredArgsConstructor
public class EmailPreferenceController {

    private final EmailContactService emailContactService;

    @Value("${app.business.company-name:}")
    private String companyName;
    @Value("${app.business.phone:}")
    private String phone;

    @GetMapping(value = "/huy-nhan-mail", produces = "text/html;charset=UTF-8")
    public String unsubscribe(@RequestParam(required = false) String token, Model model) {
        Optional<EmailContact> contact = emailContactService.unsubscribe(token);
        model.addAttribute("companyName", companyName);
        model.addAttribute("phone", phone);
        model.addAttribute("done", contact.isPresent());
        model.addAttribute("email", contact.map(EmailContact::getEmail).orElse(""));
        return "unsubscribe";
    }
}
