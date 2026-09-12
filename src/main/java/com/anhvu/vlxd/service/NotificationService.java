package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.entity.QuoteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Bao cho chu cua hang khi co don / bao gia moi. Gui nen, khong bao gio lam hong request cua khach:
 * chua cau hinh mail hoac gui loi thi chi ghi log.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final MailGateway mailGateway;
    private final String adminEmail;
    private final String siteUrl;

    public NotificationService(MailGateway mailGateway,
                               @Value("${app.security.admin-email:}") String adminEmail,
                               @Value("${app.site-url:https://vlxd-app-production.up.railway.app}") String siteUrl) {
        this.mailGateway = mailGateway;
        this.adminEmail = adminEmail;
        this.siteUrl = siteUrl;
    }

    public void newOrder(String orderCode, List<CustomerOrder> lines, BigDecimal total) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        CustomerOrder first = lines.get(0);
        StringBuilder body = new StringBuilder();
        body.append("Đơn hàng mới ").append(orderCode).append("\n\n")
                .append("Khách: ").append(first.getCustomerName()).append("\n")
                .append("Điện thoại: ").append(first.getPhone()).append("\n")
                .append("Địa chỉ: ").append(first.getAddress()).append("\n")
                .append("Thanh toán: ").append("BANK_TRANSFER".equalsIgnoreCase(first.getPaymentMethod()) ? "Chuyển khoản" : "Khi nhận hàng").append("\n\n");
        for (CustomerOrder line : lines) {
            body.append("- ").append(line.getProductName())
                    .append(" × ").append(line.getQuantity().stripTrailingZeros().toPlainString())
                    .append(" = ").append(money(line.getTotalAmount())).append("\n");
        }
        body.append("\nTạm tính: ").append(money(total)).append("\n");
        if (first.getNote() != null && !first.getNote().isBlank()) {
            body.append("Ghi chú: ").append(first.getNote()).append("\n");
        }
        body.append("\nXử lý tại: ").append(siteUrl).append("/admin?q=").append(orderCode).append("#orders\n");
        send("[VLXD Anh Vũ] Đơn mới " + orderCode + " - " + first.getCustomerName() + " - " + money(total), body.toString());
    }

    public void newQuote(QuoteRequest quote) {
        String body = "Yêu cầu báo giá mới\n\n"
                + "Khách: " + quote.getCustomerName() + "\n"
                + "Điện thoại: " + quote.getPhone() + "\n"
                + "Địa chỉ: " + (quote.getAddress() == null ? "" : quote.getAddress()) + "\n\n"
                + quote.getContent() + "\n\n"
                + "Xử lý tại: " + siteUrl + "/admin?qstatus=NEW#quotes\n";
        send("[VLXD Anh Vũ] Báo giá mới - " + quote.getCustomerName(), body);
    }

    private void send(String subject, String body) {
        if (adminEmail == null || adminEmail.isBlank() || !mailGateway.isConfigured()) {
            log.info("Bo qua email thong bao (chua cau hinh mail): {}", subject);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                mailGateway.send(adminEmail, subject, body);
            } catch (Exception e) {
                log.warn("Khong gui duoc email thong bao '{}': {}", subject, e.getMessage());
            }
        });
    }

    private static String money(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(value == null ? BigDecimal.ZERO : value) + " đ";
    }
}
