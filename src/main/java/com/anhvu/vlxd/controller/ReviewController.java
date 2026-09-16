package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.Review;
import com.anhvu.vlxd.repository.ReviewRepository;
import com.anhvu.vlxd.web.ReviewForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Khach gui danh gia tu trang chu; luu cho duyet, chua hien ngay. */
@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;

    @PostMapping("/danh-gia")
    public String store(@Valid @ModelAttribute("reviewForm") ReviewForm form, BindingResult bindingResult) {
        if (form.getWebsite() != null && !form.getWebsite().isBlank()) {
            // bot dien vao bay spam: gia vo thanh cong, khong luu
            return "redirect:/?reviewSent=1#danh-gia";
        }
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().get(0).getDefaultMessage();
            return "redirect:/?reviewError=" + URLEncoder.encode(message == null ? "Thông tin chưa hợp lệ." : message, StandardCharsets.UTF_8) + "#danh-gia";
        }
        reviewRepository.save(Review.builder()
                .customerName(form.getCustomerName().trim())
                .customerRole(form.getCustomerRole() == null ? "" : form.getCustomerRole().trim())
                .rating(form.getRating())
                .content(form.getContent().trim())
                .approved(false)
                .source("WEB")
                .build());
        return "redirect:/?reviewSent=1#danh-gia";
    }
}
