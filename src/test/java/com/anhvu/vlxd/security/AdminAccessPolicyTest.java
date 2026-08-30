package com.anhvu.vlxd.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAccessPolicyTest {

    private final AdminAccessPolicy policy = new AdminAccessPolicy("maihao0501@gmail.com");

    @Test
    void grantsAdminOnlyToConfiguredEmail() {
        assertThat(policy.isAdmin("maihao0501@gmail.com")).isTrue();
        assertThat(policy.isAdmin(" MAIHAO0501@GMAIL.COM ")).isTrue();
        assertThat(policy.isAdmin("2224802010617@student.tdmu.edu.vn")).isFalse();
        assertThat(policy.isAdmin(null)).isFalse();
    }
}
