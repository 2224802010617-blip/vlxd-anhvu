package com.anhvu.vlxd.config;

import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.entity.Payment;
import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.entity.QuoteRequest;
import com.anhvu.vlxd.entity.StockMovement;
import com.anhvu.vlxd.repository.CustomerOrderRepository;
import com.anhvu.vlxd.repository.PaymentRepository;
import com.anhvu.vlxd.repository.ProductRepository;
import com.anhvu.vlxd.repository.QuoteRequestRepository;
import com.anhvu.vlxd.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Du lieu mau cho dashboard admin (demo do an): don hang 30 ngay, thu tien, nhap/xuat kho, bao gia.
 * Khach mau co so dien thoai 09010000xx. Bien moi truong APP_DEMO_DATA:
 * on (mac dinh) = tao neu chua co, moi lan khoi dong doi ngay cho khop hom nay;
 * off = khong lam gi; remove = xoa het du lieu mau va tra lai ton kho.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer {

    static final String DEMO_PHONE_PREFIX = "09010000";
    private static final String RECEIPT_PREFIX = "Phiếu nhập PN";
    private static final String DEMO_ORDERS = "select distinct order_code from customer_orders where phone like '" + DEMO_PHONE_PREFIX + "%'";
    private static final String DEMO_MOVEMENTS = "(reference in (" + DEMO_ORDERS + ") or reference like '" + RECEIPT_PREFIX + "%')";

    private final ProductRepository productRepository;
    private final CustomerOrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StockMovementRepository movementRepository;
    private final QuoteRequestRepository quoteRepository;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    @Value("${app.demo-data:on}")
    private String mode;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String m = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        try {
            switch (m) {
                case "on" -> tx.executeWithoutResult(s -> {
                    if (demoOrderCount() > 0) refreshDates(); else seed();
                });
                case "remove" -> tx.executeWithoutResult(s -> remove());
                default -> { }
            }
        } catch (RuntimeException ex) {
            // Du lieu mau chi de trinh dien: loi o day khong duoc lam web khong len
            log.warn("Bo qua du lieu mau: {}", ex.getMessage());
        }
    }

    private long demoOrderCount() {
        Long n = jdbc.queryForObject("select count(*) from customer_orders where phone like ?", Long.class, DEMO_PHONE_PREFIX + "%");
        return n == null ? 0 : n;
    }

    /** Doi ca bo du lieu mau ve phia truoc de don moi nhat luon la hom nay. */
    private void refreshDates() {
        LocalDateTime latest = jdbc.queryForObject("select max(created_at) from customer_orders where phone like ?",
                LocalDateTime.class, DEMO_PHONE_PREFIX + "%");
        if (latest == null) return;
        long days = ChronoUnit.DAYS.between(latest.toLocalDate(), LocalDate.now());
        if (days <= 0) return;
        jdbc.update("update payments set paid_at = date_add(paid_at, interval ? day) where order_code in (" + DEMO_ORDERS + ")", days);
        jdbc.update("update stock_movements set created_at = date_add(created_at, interval ? day) where " + DEMO_MOVEMENTS, days);
        jdbc.update("update quote_requests set created_at = date_add(created_at, interval ? day) where phone like ?", days, DEMO_PHONE_PREFIX + "%");
        jdbc.update("update customer_orders set created_at = date_add(created_at, interval ? day) where phone like ?", days, DEMO_PHONE_PREFIX + "%");
        log.info("Du lieu mau: doi ngay them {} ngay cho khop hom nay", days);
    }

    private void remove() {
        // Hoan tac ton kho: xuat (so am) cong lai, nhap (so duong) tru di
        jdbc.update("update products p set stock_quantity = stock_quantity - (select coalesce(sum(m.quantity), 0) "
                + "from stock_movements m where m.product_id = p.id and " + DEMO_MOVEMENTS.replace("reference", "m.reference") + ")");
        int moves = jdbc.update("delete from stock_movements where " + DEMO_MOVEMENTS);
        jdbc.update("delete from payments where order_code in (" + DEMO_ORDERS + ")");
        int orders = jdbc.update("delete from customer_orders where phone like ?", DEMO_PHONE_PREFIX + "%");
        jdbc.update("delete from quote_requests where phone like ?", DEMO_PHONE_PREFIX + "%");
        log.info("Da xoa du lieu mau: {} dong don, {} giao dich kho", orders, moves);
    }

    // ------------------------------------------------------------------ tao moi

    private static final String[][] CUSTOMERS = {
            {"Nguyễn Văn Hùng", "Ấp Phú Bình, P. An Lộc, Bình Long"}, {"Trần Thị Mai", "Tổ 5 KP Phú Thịnh, P. Bình Long"},
            {"Lê Minh Tuấn", "Ấp 3, xã Thanh Lương, Bình Long"}, {"Phạm Quốc Bảo", "KP Phú Trung, P. Phú Đức, Bình Long"},
            {"Hoàng Văn Nam", "Ấp Sóc Bế, xã Thanh Phú, Bình Long"}, {"Võ Thị Lan", "Đường Trần Hưng Đạo, P. An Lộc"},
            {"Đặng Văn Phúc", "Ấp Hưng Thịnh, xã Thanh Lương"}, {"Bùi Thanh Sơn", "Tổ 2 KP Bình An, P. Hưng Chiến"},
            {"Đỗ Minh Khoa", "Ấp Phú Lạc, xã Thanh Phú, Bình Long"}, {"Ngô Thị Hồng", "Đường Nguyễn Huệ, P. An Lộc"},
            {"Cty XD Hưng Phát", "Công trình nhà xưởng KCN Minh Hưng"}, {"Huỳnh Văn Lợi", "Ấp Thanh Hòa, xã Thanh Lương"},
            {"Phan Thị Thu", "KP Phú Hòa, P. Hưng Chiến, Bình Long"}, {"Trương Công Danh", "Ấp 6, xã Tân Lợi, Hớn Quản"},
            {"Lý Văn Thắng", "Đường Phan Bội Châu, P. Phú Thịnh"}, {"Nhà thầu Tấn Lộc", "Công trình trường mầm non Thanh Phú"},
            {"Mai Văn Hậu", "Ấp Bào Bàng, xã Thanh Lương"}, {"Dương Thị Ngọc", "KP Phú Lộc, P. An Lộc"},
    };
    // Hang ban chay o bai va trong so
    private static final Object[][] POPULAR = {
            {"Gạch ống 8x8x18", 14}, {"Xi măng PCB40", 12}, {"Cát xây dựng", 11}, {"Thép cây gân", 10}, {"Đá 1x2 đen", 9},
            {"Gạch Tuynel", 6}, {"Xi măng Hoàng Thạch", 5}, {"Xi măng TOP", 4}, {"Cát bê tông vàng", 4}, {"Đá 4x6 đen", 4},
            {"Thép cây D10", 4}, {"Gạch block", 3}, {"Cát vàng mi", 3}, {"Đá mi sàng", 3}, {"Xi măng Nghi Sơn Premium", 3},
            {"Thép tròn trơn", 2}, {"Thép hộp oval", 2}, {"Đá 0x4 xanh", 2},
    };
    private static final String[] NOTES = {"[Giao xe tải đường lớn] Gọi trước khi giao", "[Giao hẻm nhỏ xe ba gác] Giao buổi sáng trước 10h",
            "[Cần thuê bốc vác] Đổ hàng trước cổng", "Công trình đang đổ móng", ""};

    private record Line(Product product, int qty) { }
    private record Event(LocalDateTime at, Product product, int delta, String reference, BigDecimal unitCost) { }

    private void seed() {
        Map<String, Product> byName = new LinkedHashMap<>();
        for (Product p : productRepository.findAll()) {
            if (Boolean.TRUE.equals(p.getActive()) && p.getPrice() != null && p.getPrice().signum() > 0) byName.put(p.getName(), p);
        }
        List<Object[]> popular = new ArrayList<>();
        for (Object[] row : POPULAR) if (byName.containsKey((String) row[0])) popular.add(row);
        if (popular.size() < 5) {
            log.info("Du lieu mau: khong du san pham de tao");
            return;
        }
        Random rnd = new Random(20261019);
        LocalDateTime now = LocalDateTime.now();
        for (Product p : byName.values()) {
            if (p.getCostPrice() == null) {
                double ratio = 0.82 + rnd.nextDouble() * 0.06;
                p.setCostPrice(p.getPrice().multiply(BigDecimal.valueOf(ratio)).setScale(-2, RoundingMode.HALF_UP).max(BigDecimal.ONE));
            }
        }

        // Ngay dat: rai 30 ngay, gan day nhieu don hon; gio lam viec 7h-17h
        List<LocalDateTime> dates = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            int age = Math.min(29, (int) Math.floor(30 * (1 - Math.sqrt(rnd.nextDouble()))));
            dates.add(now.toLocalDate().minusDays(age).atTime(7 + rnd.nextInt(11), rnd.nextInt(60)));
        }
        LocalDate today = now.toLocalDate();
        dates.add(today.atTime(7, 15)); dates.add(today.atTime(7, 40));
        dates.add(today.atTime(8, 5)); dates.add(today.atTime(8, 30));
        dates.sort(Comparator.naturalOrder());

        List<Event> events = new ArrayList<>();
        Map<String, Integer> forced = new LinkedHashMap<>(Map.of("Vữa chống cháy", 110, "Thép hình H150", 1700)); // co mat hang sap het
        int idx = 0;
        for (LocalDateTime created : dates) {
            if (created.isAfter(now)) created = now.minusMinutes(5);
            double age = ChronoUnit.MINUTES.between(created, now) / 1440.0;
            String status = statusFor(age, rnd);
            if (created.toLocalDate().equals(today) && created.getHour() == 7) status = "COMPLETED"; // giao som trong ngay
            int c = rnd.nextInt(CUSTOMERS.length);
            String phone = DEMO_PHONE_PREFIX + String.format("%02d", c + 1);
            String pay = rnd.nextInt(100) < 55 ? "COD" : "BANK_TRANSFER";

            List<Line> lines = new ArrayList<>();
            if ((idx == 6 || idx == 12) && !forced.isEmpty()) {
                String name = forced.keySet().iterator().next();
                int qty = forced.remove(name);
                if (byName.containsKey(name)) { lines.add(new Line(byName.get(name), qty)); status = "COMPLETED"; }
            }
            if (lines.isEmpty()) {
                int count = rnd.nextInt(100) < 50 ? 1 : rnd.nextInt(100) < 70 ? 2 : 3;
                Set<String> used = new HashSet<>();
                for (int k = 0; k < count; k++) {
                    Product p = pickPopular(popular, used, byName, rnd);
                    used.add(p.getName());
                    lines.add(new Line(p, quantityFor(p.getUnit(), rnd)));
                }
            }
            idx++;

            List<CustomerOrder> rows = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            String note = NOTES[rnd.nextInt(NOTES.length)];
            for (Line l : lines) {
                BigDecimal lineTotal = l.product().getPrice().multiply(BigDecimal.valueOf(l.qty()));
                total = total.add(lineTotal);
                rows.add(CustomerOrder.builder().customerName(CUSTOMERS[c][0]).phone(phone).address(CUSTOMERS[c][1]).email("")
                        .productName(l.product().getName()).quantity(BigDecimal.valueOf(l.qty()).setScale(2))
                        .unitPrice(l.product().getPrice()).totalAmount(lineTotal).paymentMethod(pay).note(note)
                        .status(status).createdAt(created).build());
            }
            List<CustomerOrder> saved = orderRepository.saveAll(rows);
            String code = "DH" + saved.get(0).getId();
            saved.forEach(o -> o.setOrderCode(code));
            orderRepository.saveAll(saved);

            if ("COMPLETED".equals(status)) {
                for (Line l : lines) events.add(new Event(created.plusHours(3), l.product(), -l.qty(), code, null));
            }
            LocalDateTime paidAt = created.plusHours(2 + rnd.nextInt(28));
            if (paidAt.isAfter(now)) paidAt = now.minusMinutes(10);
            if ("COMPLETED".equals(status)) {
                boolean full = rnd.nextInt(100) < 85;
                BigDecimal amount = full ? total : total.divide(BigDecimal.valueOf(2), -3, RoundingMode.HALF_UP);
                paymentRepository.save(Payment.builder().orderCode(code).amount(amount)
                        .method(full && "BANK_TRANSFER".equals(pay) ? "BANK_TRANSFER" : "CASH")
                        .note(full ? ("COD".equals(pay) ? "Thu đủ khi giao hàng" : "Khách chuyển khoản") : "Khách trả trước một nửa")
                        .paidAt(paidAt).build());
            } else if (("CONFIRMED".equals(status) || "SHIPPING".equals(status)) && "BANK_TRANSFER".equals(pay) && rnd.nextInt(100) < 60) {
                paymentRepository.save(Payment.builder().orderCode(code).amount(total).method("BANK_TRANSFER")
                        .note("Khách chuyển khoản trước").paidAt(paidAt).build());
            }
        }

        // Nhap hang tu nha cung cap (co gia von) truoc khi ban
        Object[][] receipts = {{"Xi măng PCB40", 400, 27}, {"Gạch ống 8x8x18", 20000, 25}, {"Thép cây gân", 3000, 22},
                {"Cát xây dựng", 120, 18}, {"Đá 1x2 đen", 100, 15}, {"Xi măng Hoàng Thạch", 300, 10}, {"Gạch Tuynel", 10000, 8}};
        int n = 101;
        for (Object[] r : receipts) {
            Product p = byName.get((String) r[0]);
            if (p != null) events.add(new Event(today.minusDays((int) r[2]).atTime(6, 30), p, (int) r[1], RECEIPT_PREFIX + (n++), p.getCostPrice()));
        }
        events.sort(Comparator.comparing(Event::at));
        for (Event e : events) {
            Product p = e.product();
            int after = (p.getStockQuantity() == null ? 0 : p.getStockQuantity()) + e.delta();
            if (after < 0) continue; // khong de ton am neu kho tren server it hon du kien
            p.setStockQuantity(after);
            movementRepository.save(StockMovement.builder().productId(p.getId()).productName(p.getName())
                    .type(e.delta() > 0 ? "IN" : "OUT").quantity(e.delta()).stockAfter(after)
                    .reference(e.reference()).unitCost(e.unitCost()).createdAt(e.at()).build());
        }
        productRepository.saveAll(byName.values());

        String[][] quotes = {
                {"Nguyễn Thành Long", "Ấp Phú Bình, Bình Long", "Báo giá 20.000 gạch ống + 300 bao PCB40 + 60 m3 cát xây cho nhà 2 tầng", "NEW", "0"},
                {"Cty TNHH Minh Phát", "KCN Minh Hưng 3", "Cần báo giá thép cây gân D16-D20 khoảng 12 tấn, giao 2 đợt", "NEW", "1"},
                {"Trần Văn Tài", "Xã Thanh Lương", "Đá 1x2 và cát bê tông cho đổ sàn 120 m2", "CONTACTED", "3"},
                {"Lê Thị Bích", "P. An Lộc", "Xi măng trắng + gạch block làm hàng rào 40 m", "QUOTED", "6"},
                {"Nhà thầu Hòa Bình", "Hớn Quản", "Vật tư phần thô nhà xưởng 600 m2", "CLOSED", "12"},
        };
        int q = 91;
        for (String[] r : quotes) {
            quoteRepository.save(QuoteRequest.builder().customerName(r[0]).phone(DEMO_PHONE_PREFIX + (q++)).address(r[1]).email("")
                    .content(r[2]).status(r[3]).createdAt(now.minusDays(Long.parseLong(r[4])).minusHours(2)).build());
        }
        log.info("Da tao du lieu mau dashboard: {} don", dates.size());
    }

    private static String statusFor(double ageDays, Random rnd) {
        if (ageDays < 1) return rnd.nextInt(3) < 2 ? "NEW" : "CONFIRMED";
        if (ageDays < 3) return new String[]{"CONFIRMED", "SHIPPING", "SHIPPING", "COMPLETED"}[rnd.nextInt(4)];
        if (ageDays < 6) return new String[]{"SHIPPING", "COMPLETED", "COMPLETED", "COMPLETED"}[rnd.nextInt(4)];
        return rnd.nextInt(100) < 92 ? "COMPLETED" : "CANCELED";
    }

    private static Product pickPopular(List<Object[]> popular, Set<String> used, Map<String, Product> byName, Random rnd) {
        int sum = 0;
        for (Object[] r : popular) if (!used.contains((String) r[0])) sum += (int) r[1];
        int x = rnd.nextInt(Math.max(sum, 1));
        for (Object[] r : popular) {
            if (used.contains((String) r[0])) continue;
            x -= (int) r[1];
            if (x < 0) return byName.get((String) r[0]);
        }
        return byName.get((String) popular.get(0)[0]);
    }

    private static int quantityFor(String unit, Random rnd) {
        String u = unit == null ? "" : unit.toLowerCase(Locale.ROOT);
        int[] options = u.startsWith("viên") ? new int[]{500, 1000, 1500, 2000, 3000, 4000, 5000}
                : u.startsWith("bao") ? new int[]{10, 15, 20, 30, 40, 50, 60, 80}
                : u.startsWith("m3") ? new int[]{2, 3, 4, 5, 6, 8, 10, 12}
                : u.startsWith("kg") ? new int[]{150, 200, 300, 500, 800, 1000, 1200} : new int[]{5, 10};
        return options[rnd.nextInt(options.length)];
    }
}
