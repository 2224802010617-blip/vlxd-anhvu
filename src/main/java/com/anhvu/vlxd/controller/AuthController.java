package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.AppUser;
import com.anhvu.vlxd.repository.AppUserRepository;
import com.anhvu.vlxd.service.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @GetMapping("/dang-nhap")
    public String login(@RequestParam(defaultValue = "false") boolean error,
                        @RequestParam(defaultValue = "false") boolean registered,
                        @RequestParam(defaultValue = "false") boolean resetSuccess,
                        @RequestParam(defaultValue = "false") boolean notRegistered,
                        Model model) {
        if (isLoggedIn()) {
            return "redirect:/";
        }
        model.addAttribute("error", error);
        model.addAttribute("registered", registered);
        model.addAttribute("resetSuccess", resetSuccess);
        model.addAttribute("notRegistered", notRegistered);
        model.addAttribute("googleLoginReady", isGoogleLoginReady());
        return "auth/login";
    }

    @GetMapping("/google/dang-nhap")
    public String googleLogin(HttpSession session) {
        session.setAttribute("GOOGLE_AUTH_MODE", "login");
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/google/dang-ky")
    public String googleRegister(HttpSession session) {
        session.setAttribute("GOOGLE_AUTH_MODE", "register");
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/dang-ky")
    public String registerForm(Model model) {
        if (isLoggedIn()) {
            return "redirect:/";
        }
        model.addAttribute("googleLoginReady", isGoogleLoginReady());
        return "auth/register";
    }

    @PostMapping("/dang-ky")
    public String register(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam(required = false, defaultValue = "") String phone,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           RedirectAttributes redirectAttributes) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (fullName == null || fullName.trim().length() < 2
                || normalizedEmail.isBlank()
                || password == null || password.length() < 8
                || !password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập đủ thông tin, mật khẩu tối thiểu 8 ký tự và xác nhận khớp.");
            return "redirect:/dang-ky";
        }
        if (appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            redirectAttributes.addFlashAttribute("error", "Email này đã có tài khoản.");
            return "redirect:/dang-ky";
        }

        appUserRepository.save(AppUser.builder()
                .username(normalizedEmail)
                .email(normalizedEmail)
                .fullName(fullName.trim())
                .phone(phone == null ? "" : phone.trim())
                .password(passwordEncoder.encode(password))
                .role("USER")
                .active(true)
                .build());
        return "redirect:/dang-nhap?registered=true";
    }

    @GetMapping("/quen-mat-khau")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/quen-mat-khau")
    public String forgotPassword(@RequestParam String email,
                                 RedirectAttributes redirectAttributes) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập email tài khoản.");
            return "redirect:/quen-mat-khau";
        }

        PasswordResetService.RequestStatus status = passwordResetService.requestResetCode(normalizedEmail);
        if (status == PasswordResetService.RequestStatus.MAIL_NOT_CONFIGURED) {
            redirectAttributes.addFlashAttribute("error", "Hệ thống chưa cấu hình email gửi mã xác nhận.");
            return "redirect:/quen-mat-khau";
        }
        if (status == PasswordResetService.RequestStatus.DELIVERY_FAILED) {
            redirectAttributes.addFlashAttribute("error", "Không thể gửi email lúc này. Vui lòng thử lại sau.");
            return "redirect:/quen-mat-khau";
        }

        redirectAttributes.addFlashAttribute("message", "Nếu email có tài khoản, mã xác nhận đã được gửi.");
        redirectAttributes.addAttribute("email", normalizedEmail);
        return "redirect:/xac-nhan-ma";
    }

    @GetMapping("/xac-nhan-ma")
    public String verifyResetCodeForm(@RequestParam(defaultValue = "") String email, Model model) {
        model.addAttribute("email", email.trim().toLowerCase());
        return "auth/verify-reset-code";
    }

    @PostMapping("/xac-nhan-ma")
    public String verifyResetCode(@RequestParam String email,
                                  @RequestParam String code,
                                  RedirectAttributes redirectAttributes) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        return passwordResetService.verifyCode(normalizedEmail, code)
                .map(token -> {
                    redirectAttributes.addAttribute("token", token);
                    return "redirect:/dat-lai-mat-khau";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Mã không đúng, đã hết hạn hoặc đã nhập sai quá 5 lần.");
                    redirectAttributes.addAttribute("email", normalizedEmail);
                    return "redirect:/xac-nhan-ma";
                });
    }

    @GetMapping("/dat-lai-mat-khau")
    public String resetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("validToken", passwordResetService.isResetTokenValid(token));
        return "auth/reset-password";
    }

    @PostMapping("/dat-lai-mat-khau")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        if (password == null || password.length() < 8 || !password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu tối thiểu 8 ký tự và xác nhận phải khớp.");
            redirectAttributes.addAttribute("token", token);
            return "redirect:/dat-lai-mat-khau";
        }

        if (!passwordResetService.resetPassword(token, password)) {
            redirectAttributes.addFlashAttribute("error", "Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            redirectAttributes.addAttribute("token", token);
            return "redirect:/dat-lai-mat-khau";
        }
        return "redirect:/dang-nhap?resetSuccess=true";
    }

    private boolean isLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean isGoogleLoginReady() {
        return isConfigured(googleClientId) && isConfigured(googleClientSecret);
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank() && !value.contains("CHANGE_ME");
    }
}
