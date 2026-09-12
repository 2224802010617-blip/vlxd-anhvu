package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.entity.QuoteRequest;
import com.anhvu.vlxd.web.OrderGroupView;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Tinh toan so lieu cho dashboard quan tri: KPI, chuoi bieu do, khach hang, viec can lam.
 * Doanh thu chi tinh don COMPLETED (tien thuc thu). Don NEW/CONFIRMED/SHIPPING = doanh thu du kien.
 */
@Service
@RequiredArgsConstructor
public class AdminReportService {

    public static final List<String> ORDER_STATUSES = List.of("NEW", "CONFIRMED", "SHIPPING", "COMPLETED", "CANCELED");
    public static final Map<String, String> ORDER_STATUS_LABELS = new LinkedHashMap<>();
    public static final Map<String, String> QUOTE_STATUS_LABELS = new LinkedHashMap<>();

    static {
        ORDER_STATUS_LABELS.put("NEW", "Mới");
        ORDER_STATUS_LABELS.put("CONFIRMED", "Đã xác nhận");
        ORDER_STATUS_LABELS.put("SHIPPING", "Đang giao");
        ORDER_STATUS_LABELS.put("COMPLETED", "Hoàn thành");
        ORDER_STATUS_LABELS.put("CANCELED", "Đã hủy");
        QUOTE_STATUS_LABELS.put("NEW", "Chưa phản hồi");
        QUOTE_STATUS_LABELS.put("CONTACTED", "Đã liên hệ");
        QUOTE_STATUS_LABELS.put("QUOTED", "Đã báo giá");
        QUOTE_STATUS_LABELS.put("CLOSED", "Đóng");
    }

    private final InventoryPolicy inventoryPolicy;

    // ---------- Gom dong thanh don ----------

    public List<OrderGroupView> groupOrders(List<CustomerOrder> lines) {
        Map<String, List<CustomerOrder>> byCode = new LinkedHashMap<>();
        for (CustomerOrder line : lines) {
            byCode.computeIfAbsent(line.displayCode(), key -> new ArrayList<>()).add(line);
        }
        List<OrderGroupView> groups = new ArrayList<>();
        for (Map.Entry<String, List<CustomerOrder>> entry : byCode.entrySet()) {
            List<CustomerOrder> group = entry.getValue();
            group.sort(Comparator.comparing(CustomerOrder::getId));
            CustomerOrder first = group.get(0);
            BigDecimal total = group.stream()
                    .map(line -> line.getTotalAmount() == null ? BigDecimal.ZERO : line.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            groups.add(OrderGroupView.builder()
                    .code(entry.getKey())
                    .customerName(first.getCustomerName())
                    .phone(first.getPhone())
                    .address(first.getAddress())
                    .paymentMethod(first.getPaymentMethod())
                    .note(first.getNote())
                    .status(first.getStatus() == null ? "NEW" : first.getStatus())
                    .createdAt(first.getCreatedAt())
                    .total(total)
                    .lines(group)
                    .build());
        }
        groups.sort(Comparator.comparing(OrderGroupView::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return groups;
    }

    // ---------- KPI ----------

    @Getter
    public static class Kpi {
        double revenueToday;
        double revenueThisMonth;
        double revenueLastMonth;
        Double monthGrowthPercent;      // null khi thang truoc = 0
        double pendingRevenue;
        long ordersToday;
        long newOrders;
        long confirmedOrders;
        long shippingOrders;
        long completedThisMonth;
        long totalOrders;
        double cancelRatePercent;
        long pendingQuotes;
        long lowStockCount;
        double averageOrderValue;
    }

    public Kpi computeKpi(List<OrderGroupView> groups, List<QuoteRequest> quotes, List<Product> products) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth lastMonth = thisMonth.minusMonths(1);
        Predicate<OrderGroupView> completed = group -> "COMPLETED".equalsIgnoreCase(group.getStatus());

        Kpi kpi = new Kpi();
        kpi.revenueToday = sum(groups, completed.and(group -> isOnDay(group.getCreatedAt(), today)));
        kpi.revenueThisMonth = sum(groups, completed.and(group -> isInMonth(group.getCreatedAt(), thisMonth)));
        kpi.revenueLastMonth = sum(groups, completed.and(group -> isInMonth(group.getCreatedAt(), lastMonth)));
        kpi.monthGrowthPercent = kpi.revenueLastMonth > 0
                ? (kpi.revenueThisMonth - kpi.revenueLastMonth) / kpi.revenueLastMonth * 100
                : null;
        kpi.pendingRevenue = sum(groups, group -> isPending(group.getStatus()));
        kpi.ordersToday = groups.stream().filter(group -> isOnDay(group.getCreatedAt(), today)).count();
        kpi.newOrders = countStatus(groups, "NEW");
        kpi.confirmedOrders = countStatus(groups, "CONFIRMED");
        kpi.shippingOrders = countStatus(groups, "SHIPPING");
        kpi.completedThisMonth = groups.stream().filter(completed.and(group -> isInMonth(group.getCreatedAt(), thisMonth))).count();
        kpi.totalOrders = groups.size();
        long canceled = countStatus(groups, "CANCELED");
        kpi.cancelRatePercent = groups.isEmpty() ? 0 : canceled * 100.0 / groups.size();
        kpi.pendingQuotes = quotes.stream().filter(quote -> "NEW".equalsIgnoreCase(quote.getStatus())).count();
        kpi.lowStockCount = products.stream().filter(inventoryPolicy::isLowStock).count();
        long completedCount = groups.stream().filter(completed).count();
        kpi.averageOrderValue = completedCount == 0 ? 0 : sum(groups, completed) / completedCount;
        return kpi;
    }

    // ---------- Chuoi bieu do ----------

    /** Doanh thu (hoan thanh) + so don theo ngay, 30 ngay gan nhat. */
    public Map<String, double[]> dailySeries(List<OrderGroupView> groups, int days) {
        LocalDate today = LocalDate.now();
        Map<String, double[]> series = new LinkedHashMap<>();
        DateTimeFormatter label = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = days - 1; i >= 0; i--) {
            series.put(today.minusDays(i).format(label), new double[]{0, 0});
        }
        for (OrderGroupView group : groups) {
            if (group.getCreatedAt() == null) {
                continue;
            }
            LocalDate day = group.getCreatedAt().toLocalDate();
            if (day.isBefore(today.minusDays(days - 1)) || day.isAfter(today)) {
                continue;
            }
            double[] bucket = series.get(day.format(label));
            if (bucket == null) {
                continue;
            }
            if ("COMPLETED".equalsIgnoreCase(group.getStatus())) {
                bucket[0] += group.getTotal().doubleValue();
            }
            bucket[1] += 1;
        }
        return series;
    }

    /** Doanh thu hoan thanh theo nhom hang. */
    public Map<String, Double> revenueByCategory(List<CustomerOrder> lines, List<Product> products) {
        Map<String, String> categoryOf = new LinkedHashMap<>();
        for (Product product : products) {
            if (product.getName() != null && product.getCategory() != null) {
                categoryOf.put(product.getName().toLowerCase(Locale.ROOT), product.getCategory().getName());
            }
        }
        Map<String, Double> result = new LinkedHashMap<>();
        for (CustomerOrder line : lines) {
            if (!"COMPLETED".equalsIgnoreCase(line.getStatus()) || line.getTotalAmount() == null) {
                continue;
            }
            String category = categoryOf.getOrDefault(
                    line.getProductName() == null ? "" : line.getProductName().toLowerCase(Locale.ROOT), "Khác");
            result.merge(category, line.getTotalAmount().doubleValue(), Double::sum);
        }
        return sortDesc(result);
    }

    /** Top san pham theo doanh thu hoan thanh. */
    public Map<String, Double> revenueByProduct(List<CustomerOrder> lines, int limit) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (CustomerOrder line : lines) {
            if ("COMPLETED".equalsIgnoreCase(line.getStatus()) && line.getTotalAmount() != null) {
                result.merge(line.getProductName(), line.getTotalAmount().doubleValue(), Double::sum);
            }
        }
        Map<String, Double> sorted = sortDesc(result);
        Map<String, Double> top = new LinkedHashMap<>();
        sorted.entrySet().stream().limit(limit).forEach(entry -> top.put(entry.getKey(), entry.getValue()));
        return top;
    }

    public Map<String, Long> orderStatusCounts(List<OrderGroupView> groups) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : ORDER_STATUSES) {
            counts.put(status, countStatus(groups, status));
        }
        return counts;
    }

    // ---------- Khach hang ----------

    @Getter
    public static class CustomerSummary {
        String name;
        String phone;
        String address;
        long orders;
        long pendingOrders;
        double totalSpent;
        LocalDateTime lastOrderAt;
    }

    public List<CustomerSummary> customers(List<OrderGroupView> groups, int limit) {
        Map<String, CustomerSummary> byPhone = new LinkedHashMap<>();
        for (OrderGroupView group : groups) {
            String phone = group.getPhone() == null ? "" : group.getPhone().replaceAll("\\D+", "");
            CustomerSummary summary = byPhone.computeIfAbsent(phone, key -> new CustomerSummary());
            if (summary.name == null) {   // groups da sap xep moi nhat truoc -> lay ten/dia chi moi nhat
                summary.name = group.getCustomerName();
                summary.phone = group.getPhone();
                summary.address = group.getAddress();
                summary.lastOrderAt = group.getCreatedAt();
            }
            summary.orders++;
            if (isPending(group.getStatus())) {
                summary.pendingOrders++;
            }
            if ("COMPLETED".equalsIgnoreCase(group.getStatus())) {
                summary.totalSpent += group.getTotal().doubleValue();
            }
        }
        return byPhone.values().stream()
                .sorted(Comparator.comparingDouble(CustomerSummary::getTotalSpent).reversed()
                        .thenComparing(CustomerSummary::getLastOrderAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    // ---------- Kho ----------

    public List<Product> lowStockProducts(List<Product> products) {
        return products.stream()
                .filter(inventoryPolicy::isLowStock)
                .sorted(Comparator.comparing(product -> product.getStockQuantity() == null ? 0 : product.getStockQuantity()))
                .toList();
    }

    // ---------- helpers ----------

    public static boolean isPending(String status) {
        return status == null
                || (!"COMPLETED".equalsIgnoreCase(status) && !"CANCELED".equalsIgnoreCase(status));
    }

    private static double sum(List<OrderGroupView> groups, Predicate<OrderGroupView> filter) {
        return groups.stream().filter(filter).mapToDouble(group -> group.getTotal().doubleValue()).sum();
    }

    private static long countStatus(List<OrderGroupView> groups, String status) {
        return groups.stream().filter(group -> status.equalsIgnoreCase(group.getStatus())).count();
    }

    private static boolean isOnDay(LocalDateTime time, LocalDate day) {
        return time != null && time.toLocalDate().equals(day);
    }

    private static boolean isInMonth(LocalDateTime time, YearMonth month) {
        return time != null && YearMonth.from(time).equals(month);
    }

    private static Map<String, Double> sortDesc(Map<String, Double> map) {
        Map<String, Double> sorted = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }
}
