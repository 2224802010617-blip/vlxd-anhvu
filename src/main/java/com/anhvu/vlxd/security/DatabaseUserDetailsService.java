package com.anhvu.vlxd.security;

import com.anhvu.vlxd.entity.AppUser;
import com.anhvu.vlxd.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final AdminAccessPolicy adminAccessPolicy;

    public DatabaseUserDetailsService(AppUserRepository appUserRepository,
                                      AdminAccessPolicy adminAccessPolicy) {
        this.appUserRepository = appUserRepository;
        this.adminAccessPolicy = adminAccessPolicy;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay tai khoan"));
        String role = adminAccessPolicy.isAdmin(user.getEmail()) ? "ADMIN" : "USER";
        return User.withUsername(user.getEmail())
                .password(user.getPassword())
                .disabled(Boolean.FALSE.equals(user.getActive()))
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())))
                .build();
    }
}
