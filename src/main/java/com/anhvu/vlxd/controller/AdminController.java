package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.Category;
import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.entity.QuoteRequest;
import com.anhvu.vlxd.repository.CategoryRepository;
import com.anhvu.vlxd.repository.CustomerOrderRepository;
import com.anhvu.vlxd.repository.ProductRepository;
import com.anhvu.vlxd.repository.QuoteRequestRepository;
import com.anhvu.vlxd.service.AdminReportService;
import com.anhvu.vlxd.service.InventoryPolicy;
import com.anhvu.vlxd.web.OrderGroupView;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private static final Set<String> ORDER_STATUSES = Set.of("NEW", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELED");
    private static final Set<String> QUOTE_STATUSES = Set.of("NEW", "CONTACTED", "QUOTED", "CLOSED");
    private static final int ORDER_PAGE_SIZE = 15;
    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final QuoteRequestRepository quoteRequestRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final AdminReportService reportService;
    private final InventoryPolicy inventoryPolicy;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/admin", produces = "text/html;charset=UTF-8")
    public String dashboard(@RequestParam(required = false) String status,
                            @RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(required = false) String qstatus,
                            @RequestParam(required = false) String cat,
                            @RequestParam(required = false) String pq,
                            @RequestParam(required = false) String stock,
                            Model model,
                            Authentication authentication) throws Exception {
        List<Product> products = productRepository.findAll();
        List<CustomerOrder> lines = customerOrderRepository.findAllByOrderByCreatedAtDesc();
        List<QuoteRequest> quotes = quoteRequestRepository.findAllByOrderByCreatedAtDesc();
        List<OrderGroupView> allGroups = reportService.groupOrders(lines);

        // ----- Tong quan -----
        AdminReportService.Kpi kpi = reportService.computeKpi(allGroups, quotes, products);
        List<Product> lowStock = reportService.lowStockProducts(products);
        Map<String, double[]> daily = reportService.dailySeries(allGroups, 30);
        Map<String, Double> byCategory = reportService.revenueByCategory(lines, products);
        Map<String, Double> topProducts = reportService.revenueByProduct(lines, 8);
        Map<String, Long> statusCounts = reportService.orderStatusCounts(allGroups);

        model.addAttribute("kpi", kpi);
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("todayLabel", LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"))));
        model.addAttribute("adminEmail", adminEmail(authentication));
        model.addAttribute("orderStatuses", AdminReportService.ORDER_STATUSES);
        model.addAttribute("orderStatusLabels", AdminReportService.ORDER_STATUS_LABELS);
        model.addAttribute("quoteStatuses", List.of("NEW", "CONTACTED", "QUOTED", "CLOSED"));
        model.addAttribute("quoteStatusLabels", AdminReportService.QUOTE_STATUS_LABELS);

        model.addAttribute("dailyLabelsJson", objectMapper.writeValueAsString(daily.keySet()));
        model.addAttribute("dailyRevenueJson", objectMapper.writeValueAsString(daily.values().stream().map(v -> v[0]).toList()));
        model.addAttribute("dailyOrdersJson", objectMapper.writeValueAsString(daily.values().stream().map(v -> v[1]).toList()));
        model.addAttribute("categoryLabelsJson", objectMapper.writeValueAsString(byCategory.keySet()));
        model.addAttribute("categoryDataJson", objectMapper.writeValueAsString(byCategory.values()));
        model.addAttribute("salesLabelsJson", objectMapper.writeValueAsString(topProducts.keySet()));
        model.addAttribute("salesDataJson", objectMapper.writeValueAsString(topProducts.values()));
        model.addAttribute("orderStatusLabelsJson", objectMapper.writeValueAsString(statusCounts.keySet()));
        model.addAttribute("orderStatusDataJson", objectMapper.writeValueAsString(statusCounts.values()));

        // ----- Don hang: loc + tim + phan trang -----
        String statusFilter = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String query = q == null ? "" : q.trim();
        List<OrderGroupView> filtered = allGroups.stream()
                .filter(group -> statusFilter.isEmpty() || statusFilter.equalsIgnoreCase(group.getStatus()))
                .filter(group -> query.isEmpty() || matchesOrder(group, query))
                .toList();
        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) ORDER_PAGE_SIZE));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        List<OrderGroupView> pageItems = filtered.stream()
                .skip((long) safePage * ORDER_PAGE_SIZE)
                .limit(ORDER_PAGE_SIZE)
                .toList();
        model.addAttribute("orders", pageItems);
        model.addAttribute("orderTotalFiltered", filtered.size());
        model.addAttribute("orderPage", safePage);
        model.addAttribute("orderTotalPages", totalPages);
        model.addAttribute("status", statusFilter);
        model.addAttribute("q", query);

        // ----- Bao gia -----
        String quoteFilter = qstatus == null ? "" : qstatus.trim().toUpperCase(Locale.ROOT);
        model.addAttribute("quotes", quotes.stream()
                .filter(quote -> quoteFilter.isEmpty() || quoteFilter.equalsIgnoreCase(quote.getStatus()))
                .toList());
        model.addAttribute("qstatus", quoteFilter);

        // ----- Kho -----
        String categoryFilter = cat == null ? "" : cat.trim();
        String productQuery = pq == null ? "" : pq.trim();
        boolean lowOnly = "low".equalsIgnoreCase(stock);
        List<Map<String, Object>> inventory = new ArrayList<>();
        for (Product product : products) {
            if (!categoryFilter.isEmpty() && (product.getCategory() == null
                    || !categoryFilter.equalsIgnoreCase(product.getCategory().getName()))) {
                continue;
            }
            if (!productQuery.isEmpty() && !fold(product.getName()).contains(fold(productQuery))) {
                continue;
            }
            boolean low = inventoryPolicy.isLowStock(product);
            if (lowOnly && !low) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product", product);
            row.put("low", low);
            row.put("threshold", inventoryPolicy.lowStockThreshold(product));
            row.put("service", inventoryPolicy.isService(product));
            inventory.add(row);
        }
        model.addAttribute("inventory", inventory);
        model.addAttribute("categories", categoryRepository.findAll().stream().map(Category::getName).toList());
        model.addAttribute("cat", categoryFilter);
        model.addAttribute("pq", productQuery);
        model.addAttribute("stock", lowOnly ? "low" : "");
        model.addAttribute("productCount", products.size());

        // ----- Khach hang -----
        model.addAttribute("customers", reportService.customers(allGroups, 30));
        model.addAttribute("customerCount", reportService.customers(allGroups, Integer.MAX_VALUE).size());

        return "admin/dashboard";
    }

    @PostMapping("/admin/orders/{code}/status")
    public String updateOrderStatus(@PathVariable String code,
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        if (!ORDER_STATUSES.contains(status)) {
            return "redirect:/admin#orders";
        }
        List<CustomerOrder> lines = customerOrderRepository.findByOrderCodeOrderByIdAsc(code);
        if (lines.isEmpty() && code.startsWith("DH")) {
            // Don cu chua co orderCode: DH + id
            try {
                customerOrderRepository.findById(Long.parseLong(code.substring(2))).ifPresent(lines::add);
            } catch (NumberFormatException ignored) {
                // ma khong hop le -> khong cap nhat
            }
        }
        for (CustomerOrder line : lines) {
            line.setStatus(status);
        }
        customerOrderRepository.saveAll(lines);
        if (!lines.isEmpty()) {
            redirectAttributes.addFlashAttribute("adminSuccess",
                    "Đơn " + code + " → " + AdminReportService.ORDER_STATUS_LABELS.getOrDefault(status, status) + ".");
        }
        return "redirect:/admin#orders";
    }

    /** Xoa ca don (moi dong hang cung ma). Dung de don don test / don nhap nham. */
    @PostMapping("/admin/orders/{code}/delete")
    public String deleteOrder(@PathVariable String code, RedirectAttributes redirectAttributes) {
        List<CustomerOrder> lines = customerOrderRepository.findByOrderCodeOrderByIdAsc(code);
        if (lines.isEmpty() && code.startsWith("DH")) {
            try {
                customerOrderRepository.findById(Long.parseLong(code.substring(2))).ifPresent(lines::add);
            } catch (NumberFormatException ignored) {
                // ma khong hop le -> khong xoa
            }
        }
        if (lines.isEmpty()) {
            redirectAttributes.addFlashAttribute("adminError", "Không tìm thấy đơn " + code + ".");
        } else {
            customerOrderRepository.deleteAll(lines);
            redirectAttributes.addFlashAttribute("adminSuccess", "Đã xóa đơn " + code + " (" + lines.size() + " dòng hàng).");
        }
        return "redirect:/admin#orders";
    }

    @PostMapping("/admin/quotes/{id}/status")
    public String updateQuoteStatus(@PathVariable Long id,
                                    @RequestParam String status) {
        if (QUOTE_STATUSES.contains(status)) {
            quoteRequestRepository.findById(id).ifPresent(quote -> {
                quote.setStatus(status);
                quoteRequestRepository.save(quote);
            });
        }
        return "redirect:/admin#quotes";
    }

    @PostMapping("/admin/products/{id}/inventory")
    public String updateInventory(@PathVariable Long id,
                                  @RequestParam Integer stockQuantity,
                                  @RequestParam BigDecimal price,
                                  @RequestParam(required = false) Boolean active,
                                  RedirectAttributes redirectAttributes) {
        if (stockQuantity == null || stockQuantity < 0 || price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("adminError", "Tồn kho và giá phải lớn hơn hoặc bằng 0.");
            return "redirect:/admin#inventory";
        }

        productRepository.findById(id).ifPresent(product -> {
            product.setStockQuantity(stockQuantity);
            product.setPrice(price);
            product.setActive(Boolean.TRUE.equals(active));
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("adminSuccess", "Đã cập nhật " + product.getName() + ".");
        });
        return "redirect:/admin#inventory";
    }

    /** Xuat danh sach don (moi dong hang mot dong) ra CSV mo duoc bang Excel. */
    @GetMapping("/admin/orders/export.csv")
    public ResponseEntity<byte[]> exportOrders(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) String q) {
        String statusFilter = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String query = q == null ? "" : q.trim();
        List<OrderGroupView> groups = reportService.groupOrders(customerOrderRepository.findAllByOrderByCreatedAtDesc()).stream()
                .filter(group -> statusFilter.isEmpty() || statusFilter.equalsIgnoreCase(group.getStatus()))
                .filter(group -> query.isEmpty() || matchesOrder(group, query))
                .toList();

        StringBuilder csv = new StringBuilder("﻿");   // BOM de Excel doc dung tieng Viet
        csv.append("Mã đơn;Ngày đặt;Khách hàng;Số điện thoại;Địa chỉ;Sản phẩm;Số lượng;Đơn giá;Thành tiền;Tổng đơn;Thanh toán;Trạng thái;Ghi chú\n");
        for (OrderGroupView group : groups) {
            for (CustomerOrder line : group.getLines()) {
                csv.append(String.join(";",
                        csvCell(group.getCode()),
                        csvCell(group.getCreatedAt() == null ? "" : group.getCreatedAt().format(CSV_DATE)),
                        csvCell(group.getCustomerName()),
                        csvCell("'" + group.getPhone()),
                        csvCell(group.getAddress()),
                        csvCell(line.getProductName()),
                        csvCell(plain(line.getQuantity())),
                        csvCell(plain(line.getUnitPrice())),
                        csvCell(plain(line.getTotalAmount())),
                        csvCell(plain(group.getTotal())),
                        csvCell(group.getPaymentMethod()),
                        csvCell(AdminReportService.ORDER_STATUS_LABELS.getOrDefault(group.getStatus(), group.getStatus())),
                        csvCell(group.getNote())
                )).append('\n');
            }
        }
        String fileName = "don-hang-anhvu-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private boolean matchesOrder(OrderGroupView group, String query) {
        String needle = fold(query);
        if (fold(group.getCode()).contains(needle)
                || fold(group.getCustomerName()).contains(needle)
                || fold(group.getPhone()).contains(needle)
                || fold(group.getAddress()).contains(needle)) {
            return true;
        }
        return group.getLines().stream().anyMatch(line -> fold(line.getProductName()).contains(needle));
    }

    /** Bo dau, chu thuong de tim kiem "xi mang" ra "Xi măng". */
    private static String fold(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd');
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String csvCell(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
        return "\"" + safe + "\"";
    }

    private String adminEmail(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        return authentication.getName();
    }
}
