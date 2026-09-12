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
 * Email cua he thong: bao chu cua hang khi co don / bao gia moi, va gui xac nhan cho khach.
 * Gui nen; chua cau hinh mail hoac gui loi thi chi ghi log, khong lam hong luong dat hang.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final MailGateway mailGateway;
    private final String adminEmail;
    private final String siteUrl;
    private final String companyName;
    private final String bankAccount;
    private final String bankName;
    private final String hotline;

    public NotificationService(MailGateway mailGateway,
                               @Value("${app.security.admin-email:}") String adminEmail,
                               @Value("${app.site-url:https://vlxd-app-production.up.railway.app}") String siteUrl,
                               @Value("${app.business.company-name:}") String companyName,
                               @Value("${app.business.bank-account:}") String bankAccount,
                               @Value("${app.business.bank-name:}") String bankName,
                               @Value("${app.business.phone:}") String hotline) {
        this.mailGateway = mailGateway;
        this.adminEmail = adminEmail;
        this.siteUrl = siteUrl;
        this.companyName = companyName;
        this.bankAccount = bankAccount;
        this.bankName = bankName;
        this.hotline = hotline;
    }

    // ---------- Gui cho chu cua hang ----------

    public void newOrder(String orderCode, List<CustomerOrder> lines, BigDecimal total) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        CustomerOrder first = lines.get(0);
        StringBuilder body = new StringBuilder();
        body.append("Đơn hàng mới ").append(orderCode).append("\n\n")
                .append("Khách: ").append(first.getCustomerName()).append("\n")
                .append("Điện thoại: ").append(first.getPhone()).append("\n")
                .append("Địa chỉ: ").append(first.getAddress()).append("\n");
        if (first.getEmail() != null && !first.getEmail().isBlank()) {
            body.append("Email khách: ").append(first.getEmail()).append("\n");
        }
        body.append("Thanh toán: ")
                .append("BANK_TRANSFER".equalsIgnoreCase(first.getPaymentMethod()) ? "Chuyển khoản" : "Khi nhận hàng")
                .append("\n\n");
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
        sendTo(adminEmail,
                "[VLXD Anh Vũ] Đơn mới " + orderCode + " - " + first.getCustomerName() + " - " + money(total),
                body.toString());
    }

    public void newQuote(QuoteRequest quote) {
        String body = "Yêu cầu báo giá mới\n\n"
                + "Khách: " + quote.getCustomerName() + "\n"
                + "Điện thoại: " + quote.getPhone() + "\n"
                + "Địa chỉ: " + (quote.getAddress() == null ? "" : quote.getAddress()) + "\n\n"
                + quote.getContent() + "\n\n"
                + "Xử lý tại: " + siteUrl + "/admin?qstatus=NEW#quotes\n";
        sendTo(adminEmail, "[VLXD Anh Vũ] Báo giá mới - " + quote.getCustomerName(), body);
    }

    // ---------- Gui cho khach ----------

    /**
     * Xac nhan don hang gui cho khach (chi khi khach co nhap email).
     * units: don vi tinh cua tung dong hang, cung thu tu voi lines.
     */
    public void orderConfirmationToCustomer(String orderCode,
                                            String customerEmail,
                                            List<CustomerOrder> lines,
                                            List<String> units,
                                            BigDecimal total,
                                            boolean bankTransfer,
                                            String paymentContent) {
        if (customerEmail == null || customerEmail.isBlank() || lines == null || lines.isEmpty()) {
            return;
        }
        CustomerOrder first = lines.get(0);
        StringBuilder body = new StringBuilder();
        body.append("Chào ").append(first.getCustomerName()).append(",\n\n")
                .append(companyName).append(" đã nhận được đơn hàng của quý khách.\n\n")
                .append("MÃ ĐƠN: ").append(orderCode).append("\n")
                .append("Giao đến: ").append(first.getAddress()).append("\n")
                .append("Điện thoại: ").append(first.getPhone()).append("\n\n")
                .append("HÀNG ĐÃ ĐẶT\n");
        for (int i = 0; i < lines.size(); i++) {
            CustomerOrder line = lines.get(i);
            String unit = units != null && i < units.size() && units.get(i) != null ? " " + units.get(i) : "";
            body.append("- ").append(line.getProductName())
                    .append(": ").append(line.getQuantity().stripTrailingZeros().toPlainString()).append(unit)
                    .append(" x ").append(money(line.getUnitPrice()))
                    .append(" = ").append(money(line.getTotalAmount())).append("\n");
        }
        body.append("\nTạm tính: ").append(money(total)).append("\n")
                .append("(Chưa gồm phí vận chuyển và bốc xếp. Nhân viên sẽ gọi báo cước chính xác theo địa chỉ giao.)\n\n");

        if (bankTransfer && total.signum() > 0) {
            body.append("THANH TOÁN CHUYỂN KHOẢN\n")
                    .append("  Ngân hàng: ").append(bankName).append("\n")
                    .append("  Số tài khoản: ").append(bankAccount).append("\n")
                    .append("  Chủ tài khoản: ").append(companyName).append("\n")
                    .append("  Số tiền: ").append(money(total)).append("\n")
                    .append("  Nội dung: ").append(paymentContent).append("\n\n");
        } else {
            body.append("Thanh toán: tiền mặt khi nhận hàng.\n\n");
        }

        body.append("Tra cứu tình trạng đơn bất kỳ lúc nào tại ")
                .append(siteUrl).append("/tra-cuu-don-hang (nhập số điện thoại đã đặt).\n\n")
                .append("Cần hỗ trợ, quý khách gọi ").append(hotline).append(".\n\n")
                .append("Trân trọng,\n").append(companyName).append("\n")
                .append("617 Nguyễn Huệ, P. Bình Long, TP. Đồng Nai\n");

        sendTo(customerEmail.trim(),
                "[VLXD Anh Vũ] Xác nhận đơn hàng " + orderCode + " - " + money(total),
                body.toString());
    }

    // ---------- helpers ----------

    /** Gui nen, loi chi ghi log de khong anh huong luong dat hang cua khach. */
    private void sendTo(String to, String subject, String body) {
        if (to == null || to.isBlank() || !mailGateway.isConfigured()) {
            log.info("Bo qua email (thieu nguoi nhan hoac chua cau hinh mail): {}", subject);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                mailGateway.send(to, subject, body);
            } catch (Exception e) {
                log.warn("Khong gui duoc email '{}' toi {}: {}", subject, to, e.getMessage());
            }
        });
    }

    private static String money(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(value == null ? BigDecimal.ZERO : value) + " đ";
    }
}
