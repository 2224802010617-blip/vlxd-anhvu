package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.Category;
import com.anhvu.vlxd.entity.Payment;
import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.entity.QuoteRequest;
import com.anhvu.vlxd.repository.CategoryRepository;
import com.anhvu.vlxd.repository.CustomerOrderRepository;
import com.anhvu.vlxd.repository.PaymentRepository;
import com.anhvu.vlxd.repository.ProductRepository;
import com.anhvu.vlxd.repository.QuoteRequestRepository;
import com.anhvu.vlxd.service.AdminReportService;
import com.anhvu.vlxd.service.InventoryPolicy;
import com.anhvu.vlxd.service.InventoryService;
import com.anhvu.vlxd.service.ProductImageService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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
    private static final int INVENTORY_PAGE_SIZE = 12;
    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final QuoteRequestRepository quoteRequestRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final AdminReportService reportService;
    private final InventoryPolicy inventoryPolicy;
    private final ProductImageService productImageService;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/admin", produces = "text/html;charset=UTF-8")
    public String dashboard(@RequestParam(required = false) String status,
                            @RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(required = false) String qstatus,
                            @RequestParam(required = false) String cat,
                            @RequestParam(required = false) String pq,
                            @RequestParam(required = false) String stock,
                            @RequestParam(required = false) String debt,
                            @RequestParam(defaultValue = "0") int ppage,
                            Model model,
                            Authentication authentication) throws Exception {
        List<Product> products = productRepository.findAll();
        List<CustomerOrder> lines = customerOrderRepository.findAllByOrderByCreatedAtDesc();
        List<QuoteRequest> quotes = quoteRequestRepository.findAllByOrderByCreatedAtDesc();
        List<OrderGroupView> allGroups = reportService.groupOrders(lines);
        reportService.attachPayments(allGroups, paymentRepository.findAll());

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
                .filter(group -> !"1".equals(debt) || group.getReceivable().signum() > 0)
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
        model.addAttribute("debt", "1".equals(debt) ? "1" : "");

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
        int inventoryPages = Math.max(1, (int) Math.ceil(inventory.size() / (double) INVENTORY_PAGE_SIZE));
        int safeInventoryPage = Math.min(Math.max(ppage, 0), inventoryPages - 1);
        model.addAttribute("inventoryTotal", inventory.size());
        model.addAttribute("inventoryPage", safeInventoryPage);
        model.addAttribute("inventoryPages", inventoryPages);
        model.addAttribute("inventory", inventory.stream()
                .skip((long) safeInventoryPage * INVENTORY_PAGE_SIZE)
                .limit(INVENTORY_PAGE_SIZE)
                .toList());
        model.addAttribute("categories", categoryRepository.findAll().stream().map(Category::getName).toList());
        model.addAttribute("cat", categoryFilter);
        model.addAttribute("pq", productQuery);
        model.addAttribute("stock", lowOnly ? "low" : "");
        model.addAttribute("productCount", products.size());
        model.addAttribute("movements", inventoryService.recent());
        model.addAttribute("units", List.of("viên", "bao", "kg", "tấn", "m3", "cây", "tấm", "báo giá"));

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
        boolean wasCompleted = !lines.isEmpty() && "COMPLETED".equalsIgnoreCase(lines.get(0).getStatus());
        for (CustomerOrder line : lines) {
            line.setStatus(status);
        }
        customerOrderRepository.saveAll(lines);
        // Ton kho di theo trang thai: hoan thanh -> tru kho; roi khoi hoan thanh -> tra lai kho
        if (!lines.isEmpty() && "COMPLETED".equals(status) && !wasCompleted) {
            inventoryService.applyCompletion(code, lines);
        } else if (!lines.isEmpty() && !"COMPLETED".equals(status) && wasCompleted) {
            inventoryService.revertCompletion(code, lines);
        }
        if (!lines.isEmpty()) {
            redirectAttributes.addFlashAttribute("adminSuccess",
                    "Đơn " + code + " → " + AdminReportService.ORDER_STATUS_LABELS.getOrDefault(status, status) + ".");
        }
        return "redirect:/admin#orders";
    }

    /** Ghi nhan thu tien cho don. amount trong = thu du phan con lai. */
    @PostMapping("/admin/orders/{code}/payments")
    public String addPayment(@PathVariable String code,
                             @RequestParam(required = false) BigDecimal amount,
                             @RequestParam(defaultValue = "CASH") String method,
                             @RequestParam(required = false) String note,
                             RedirectAttributes redirectAttributes) {
        List<CustomerOrder> lines = findOrderLines(code);
        if (lines.isEmpty()) {
            redirectAttributes.addFlashAttribute("adminError", "Không tìm thấy đơn " + code + ".");
            return "redirect:/admin#orders";
        }
        List<OrderGroupView> group = reportService.groupOrders(lines);
        reportService.attachPayments(group, paymentRepository.findByOrderCodeOrderByPaidAtAsc(code));
        BigDecimal debt = group.get(0).getDebt();
        BigDecimal toPay = amount == null || amount.signum() <= 0 ? debt : amount;
        if (toPay.signum() <= 0) {
            redirectAttributes.addFlashAttribute("adminError", "Đơn " + code + " đã thu đủ.");
            return "redirect:/admin?q=" + code + "#orders";
        }
        paymentRepository.save(Payment.builder()
                .orderCode(code)
                .amount(toPay.setScale(0, java.math.RoundingMode.HALF_UP))
                .method("BANK_TRANSFER".equals(method) ? "BANK_TRANSFER" : "CASH")
                .note(note == null ? "" : note.trim())
                .build());
        redirectAttributes.addFlashAttribute("adminSuccess",
                "Đã ghi thu " + String.format(new Locale("vi", "VN"), "%,.0f", toPay) + " ₫ cho đơn " + code + ".");
        return "redirect:/admin?q=" + code + "#orders";
    }

    /** Phieu giao hang kiem phieu thu, mo tab moi de in. */
    @GetMapping(value = "/admin/orders/{code}/phieu", produces = "text/html;charset=UTF-8")
    public String deliveryNote(@PathVariable String code, Model model) {
        List<CustomerOrder> lines = findOrderLines(code);
        if (lines.isEmpty()) {
            return "redirect:/admin?q=" + code + "#orders";
        }
        List<OrderGroupView> group = reportService.groupOrders(lines);
        List<Payment> payments = paymentRepository.findByOrderCodeOrderByPaidAtAsc(code);
        reportService.attachPayments(group, payments);
        Map<String, String> unitOf = new LinkedHashMap<>();
        for (Product product : productRepository.findAll()) {
            if (product.getName() != null) {
                unitOf.put(product.getName().toLowerCase(Locale.ROOT), product.getUnit() == null ? "" : product.getUnit());
            }
        }
        model.addAttribute("order", group.get(0));
        model.addAttribute("payments", payments);
        model.addAttribute("unitOf", unitOf);
        model.addAttribute("statusLabel", AdminReportService.ORDER_STATUS_LABELS.getOrDefault(group.get(0).getStatus(), group.get(0).getStatus()));
        model.addAttribute("printedAt", java.time.LocalDateTime.now());
        return "admin/delivery-note";
    }

    /** Nhap kho: tang ton + ghi gia von + lich su. */
    @PostMapping("/admin/products/{id}/receive")
    public String receiveStock(@PathVariable Long id,
                               @RequestParam Integer quantity,
                               @RequestParam(required = false) BigDecimal unitCost,
                               @RequestParam(required = false) String note,
                               RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null || quantity == null || quantity <= 0) {
            redirectAttributes.addFlashAttribute("adminError", "Số lượng nhập phải lớn hơn 0.");
            return "redirect:/admin#inventory";
        }
        inventoryService.receive(product, quantity, unitCost, note);
        redirectAttributes.addFlashAttribute("adminSuccess",
                "Đã nhập " + quantity + " " + product.getUnit() + " " + product.getName() + " (tồn mới: " + product.getStockQuantity() + ").");
        return "redirect:/admin?pq=" + urlEncode(product.getName()) + "#inventory";
    }

    private List<CustomerOrder> findOrderLines(String code) {
        List<CustomerOrder> lines = new ArrayList<>(customerOrderRepository.findByOrderCodeOrderByIdAsc(code));
        if (lines.isEmpty() && code.startsWith("DH")) {
            try {
                customerOrderRepository.findById(Long.parseLong(code.substring(2))).ifPresent(lines::add);
            } catch (NumberFormatException ignored) {
                // ma khong hop le
            }
        }
        return lines;
    }

    /** Xoa ca don (moi dong hang cung ma). Dung de don don test / don nhap nham. */
    @org.springframework.transaction.annotation.Transactional
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
            paymentRepository.deleteByOrderCode(code);
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
                                  @RequestParam(required = false) MultipartFile image,
                                  @RequestParam(required = false) String name,
                                  @RequestParam(required = false) String description,
                                  RedirectAttributes redirectAttributes) {
        if (stockQuantity == null || stockQuantity < 0 || price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("adminError", "Tồn kho và giá phải lớn hơn hoặc bằng 0.");
            return "redirect:/admin#inventory";
        }

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("adminError", "Không tìm thấy sản phẩm.");
            return "redirect:/admin#inventory";
        }
        inventoryService.adjust(product, stockQuantity, null);
        product.setPrice(price);
        product.setActive(Boolean.TRUE.equals(active));
        if (name != null && !name.isBlank()) {
            product.setName(name.trim());
        }
        if (description != null) {
            product.setDescription(description.trim());
        }
        if (productImageService.isUsable(image)) {
            try {
                product.setImagePath(productImageService.store(product.getId(), image));
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("adminError", "Ảnh không hợp lệ: " + e.getMessage());
                return "redirect:/admin#inventory";
            }
        }
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("adminSuccess", "Đã cập nhật " + product.getName() + ".");
        return "redirect:/admin#inventory";
    }

    /** Them san pham moi. Nhom hang: chon san co hoac go ten nhom moi. */
    @PostMapping("/admin/products")
    public String createProduct(@RequestParam String name,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String newCategory,
                                @RequestParam BigDecimal price,
                                @RequestParam String unit,
                                @RequestParam Integer stockQuantity,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) BigDecimal consumptionPerM2,
                                @RequestParam(required = false) MultipartFile image,
                                RedirectAttributes redirectAttributes) {
        String cleanName = name == null ? "" : name.trim();
        String categoryName = newCategory != null && !newCategory.isBlank() ? newCategory.trim()
                : (category == null ? "" : category.trim());
        if (cleanName.isEmpty() || categoryName.isEmpty() || unit == null || unit.isBlank()
                || price == null || price.signum() < 0 || stockQuantity == null || stockQuantity < 0) {
            redirectAttributes.addFlashAttribute("adminError", "Thiếu tên, nhóm, đơn vị, hoặc giá/tồn kho âm.");
            return "redirect:/admin#add-product";
        }
        boolean duplicated = productRepository.findAll().stream()
                .anyMatch(product -> product.getName() != null && product.getName().equalsIgnoreCase(cleanName));
        if (duplicated) {
            redirectAttributes.addFlashAttribute("adminError", "Đã có sản phẩm tên \"" + cleanName + "\".");
            return "redirect:/admin#add-product";
        }

        Category categoryEntity = categoryRepository.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(Category.builder().name(categoryName).build()));

        Product product = productRepository.save(Product.builder()
                .name(cleanName)
                .description(description == null ? "" : description.trim())
                .price(price)
                .unit(unit.trim())
                .stockQuantity(stockQuantity)
                .consumptionPerM2(consumptionPerM2)
                .category(categoryEntity)
                .imagePath(defaultImageFor(categoryName))
                .active(true)
                .build());

        if (productImageService.isUsable(image)) {
            try {
                product.setImagePath(productImageService.store(product.getId(), image));
                productRepository.save(product);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("adminError",
                        "Đã thêm " + cleanName + " nhưng ảnh không hợp lệ: " + e.getMessage() + ". Bạn đổi ảnh lại trong bảng kho.");
                return "redirect:/admin?pq=" + urlEncode(cleanName) + "#inventory";
            }
        }
        redirectAttributes.addFlashAttribute("adminSuccess", "Đã thêm sản phẩm " + cleanName + ".");
        return "redirect:/admin?pq=" + urlEncode(cleanName) + "#inventory";
    }

    /** Xoa san pham. Don hang luu ten san pham dang chu nen lich su don khong bi anh huong. */
    @PostMapping("/admin/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productRepository.findById(id).ifPresentOrElse(product -> {
            productImageService.delete(product.getId());
            productRepository.delete(product);
            redirectAttributes.addFlashAttribute("adminSuccess", "Đã xóa sản phẩm " + product.getName() + ".");
        }, () -> redirectAttributes.addFlashAttribute("adminError", "Không tìm thấy sản phẩm."));
        return "redirect:/admin#inventory";
    }

    /** Anh mac dinh theo nhom khi admin chua tai anh len. */
    private static String defaultImageFor(String categoryName) {
        String folded = fold(categoryName);
        if (folded.contains("gach")) return "/images/materials/brick.jpg";
        if (folded.contains("xi mang")) return "/images/materials/cement.jpg";
        if (folded.contains("cat")) return "/images/materials/sand.jpg";
        if (folded.contains("thep")) return "/images/materials/steel.jpg";
        if (folded.contains("da")) return "/images/materials/warehouse.jpg";
        return "/images/materials/hero-construction.jpg";
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Xuat danh sach don (moi dong hang mot dong) ra CSV mo duoc bang Excel. */
    @GetMapping("/admin/orders/export.csv")
    public ResponseEntity<byte[]> exportOrders(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) String q) {
        String statusFilter = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String query = q == null ? "" : q.trim();
        List<OrderGroupView> allForExport = reportService.groupOrders(customerOrderRepository.findAllByOrderByCreatedAtDesc());
        reportService.attachPayments(allForExport, paymentRepository.findAll());
        List<OrderGroupView> groups = allForExport.stream()
                .filter(group -> statusFilter.isEmpty() || statusFilter.equalsIgnoreCase(group.getStatus()))
                .filter(group -> query.isEmpty() || matchesOrder(group, query))
                .toList();

        StringBuilder csv = new StringBuilder("﻿");   // BOM de Excel doc dung tieng Viet
        csv.append("Mã đơn;Ngày đặt;Khách hàng;Số điện thoại;Địa chỉ;Sản phẩm;Số lượng;Đơn giá;Thành tiền;Tổng đơn;Đã thu;Còn nợ;Thanh toán;Trạng thái;Ghi chú\n");
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
                        csvCell(plain(group.getPaid())),
                        csvCell(plain(group.getDebt())),
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
