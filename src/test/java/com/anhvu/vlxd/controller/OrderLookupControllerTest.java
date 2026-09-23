package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.repository.CustomerOrderRepository;
import com.anhvu.vlxd.service.AdminReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrderLookupControllerTest {
    private final OrderLookupController controller = new OrderLookupController(
            mock(CustomerOrderRepository.class), mock(AdminReportService.class));

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void signedInAccountSurvivesGetAndPost() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer@example.test", "", List.of()));
        ExtendedModelMap model = new ExtendedModelMap();
        controller.lookupPage(null, model);
        assertThat(model.get("authenticated")).isEqualTo(true);
        assertThat(model.get("currentEmail")).isEqualTo("customer@example.test");
        controller.lookup("", model);
        assertThat(model.get("authenticated")).isEqualTo(true);
    }

    @Test void guestStillSeesLogin() {
        ExtendedModelMap model = new ExtendedModelMap();
        controller.lookupPage(null, model);
        assertThat(model.get("authenticated")).isEqualTo(false);
    }
}
