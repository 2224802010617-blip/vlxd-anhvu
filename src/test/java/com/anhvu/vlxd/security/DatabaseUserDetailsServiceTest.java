package com.anhvu.vlxd.security;

import com.anhvu.vlxd.entity.AppUser;
import com.anhvu.vlxd.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    private final AdminAccessPolicy policy = new AdminAccessPolicy("maihao0501@gmail.com");

    @Test
    void ignoresStoredAdminRoleForAnotherEmail() {
        AppUser user = user("student@example.com", "ADMIN");
        when(appUserRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails details = new DatabaseUserDetailsService(appUserRepository, policy)
                .loadUserByUsername(user.getEmail());

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void grantsAdminToConfiguredEmailEvenIfStoredRoleIsUser() {
        AppUser user = user("maihao0501@gmail.com", "USER");
        when(appUserRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails details = new DatabaseUserDetailsService(appUserRepository, policy)
                .loadUserByUsername(user.getEmail());

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    private AppUser user(String email, String role) {
        return AppUser.builder()
                .id(1L)
                .username(email)
                .email(email)
                .password("encoded-password")
                .fullName("Test User")
                .role(role)
                .active(true)
                .build();
    }
}
