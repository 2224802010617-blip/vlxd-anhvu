package com.anhvu.vlxd.config;

import com.anhvu.vlxd.entity.AppUser;
import com.anhvu.vlxd.repository.AppUserRepository;
import com.anhvu.vlxd.security.AdminAccessPolicy;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService) throws Exception {
        http
                .csrf(csrf -> csrf
                        .requireCsrfProtectionMatcher(request ->
                                request.getServletPath().startsWith("/admin")
                                        && !"GET".equalsIgnoreCase(request.getMethod())
                                        && !"HEAD".equalsIgnoreCase(request.getMethod()))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/", "/calculate", "/quote-request", "/order-request", "/dat-hang",
                                "/dang-nhap", "/dang-ky", "/quen-mat-khau", "/xac-nhan-ma", "/dat-lai-mat-khau",
                                "/google/dang-nhap", "/google/dang-ky",
                                "/api/chat/**",
                                "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/dang-nhap")
                        .loginProcessingUrl("/dang-nhap")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler())
                        .failureUrl("/dang-nhap?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/dang-nhap")
                        .successHandler(loginSuccessHandler())
                        .failureUrl("/dang-nhap?notRegistered=true")
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                );
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            response.sendRedirect(admin ? "/admin" : "/");
        };
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(AdminAccessPolicy adminAccessPolicy,
                                                                 AppUserRepository appUserRepository,
                                                                 PasswordEncoder passwordEncoder) {
        OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            String email = oidcUser.getEmail();
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("Khong lay duoc email tu Google account");
            }
            String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
            String displayName = oidcUser.getClaimAsString("name");
            if (displayName == null || displayName.isBlank()) {
                displayName = normalizedEmail;
            }

            HttpSession session = currentSession();
            String authMode = session == null ? "login" : String.valueOf(session.getAttribute("GOOGLE_AUTH_MODE"));
            if (session != null) {
                session.removeAttribute("GOOGLE_AUTH_MODE");
            }

            Optional<AppUser> optionalUser = appUserRepository.findByEmailIgnoreCase(normalizedEmail);
            if (optionalUser.isEmpty() && !"register".equals(authMode)) {
                throw new OAuth2AuthenticationException(new OAuth2Error(
                        "account_not_registered",
                        "Tai khoan Google nay chua duoc dang ky.",
                        null));
            }

            AppUser user = optionalUser.orElseGet(AppUser::new);
            boolean admin = adminAccessPolicy.isAdmin(normalizedEmail);

            user.setUsername(normalizedEmail);
            user.setEmail(normalizedEmail);
            user.setFullName(displayName);
            user.setRole(admin ? "ADMIN" : "USER");
            user.setActive(true);
            if (user.getId() == null) {
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            }
            appUserRepository.save(user);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            if (admin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
        };
    }

    private HttpSession currentSession() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getSession(false);
        }
        return null;
    }
}
