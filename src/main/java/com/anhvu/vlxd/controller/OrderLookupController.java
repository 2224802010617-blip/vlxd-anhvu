package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.repository.CustomerOrderRepository;
import com.anhvu.vlxd.service.AdminReportService;
import com.anhvu.vlxd.web.OrderGroupView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tra-cuu-don-hang")
@RequiredArgsConstructor
public class OrderLookupController {

    private static final Map<String, String> STATUS_LABELS = new LinkedHashMap<>(Map.of(
            "NEW", "Chờ xác nhận",
            "CONFIRMED", "Đã xác nhận",
            "SHIPPING", "Đang giao hàng",
            "COMPLETED", "Hoàn tất",
            "CANCELED", "Đã hủy"
    ));

    private final CustomerOrderRepository customerOrderRepository;
    private final AdminReportService reportService;

    @org.springframework.beans.factory.annotation.Value("${app.business.zalo-url:}")
    private String zaloUrl;

    @GetMapping
    public String lookupPage(@RequestParam(required = false) String phone, Model model) {
        addCommonAttributes(model);
        model.addAttribute("searched", false);
        model.addAttribute("phone", phone == null ? "" : phone);
        return "order-lookup";
    }

    @PostMapping
    public String lookup(@RequestParam String phone, Model model) {
        addCommonAttributes(model);
        String normalized = phone == null ? "" : phone.trim().replaceAll("\\s+", "");
        model.addAttribute("phone", normalized);
        model.addAttribute("searched", true);

        if (normalized.isEmpty()) {
            model.addAttribute("lookupError", "Vui lòng nhập số điện thoại đã dùng khi đặt hàng.");
            return "order-lookup";
        }

        List<CustomerOrder> lines = customerOrderRepository.findByPhoneOrderByCreatedAtDesc(normalized);
        List<OrderGroupView> orders = reportService.groupOrders(lines);
        model.addAttribute("orders", orders);
        model.addAttribute("statusLabels", STATUS_LABELS);
        model.addAttribute("dateFormatter", DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        return "order-lookup";
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("companyName", "CÔNG TY TNHH MTV TM DV XD ANH VŨ");
        model.addAttribute("email", "xaydungvantaidaphuongthucanhvu@gmail.com");
        model.addAttribute("zalo", zaloUrl == null ? "" : zaloUrl.trim());
        model.addAttribute("zaloReady", zaloUrl != null && !zaloUrl.isBlank());
        model.addAttribute("authenticated", false);
        model.addAttribute("currentEmail", "");
    }
}
