package com.anhvu.vlxd.web;

import com.anhvu.vlxd.entity.CustomerOrder;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Mot lan dat hang cua khach = nhieu dong customer_orders co cung orderCode. */
@Getter
@Builder
public class OrderGroupView {
    private final String code;
    private final String customerName;
    private final String phone;
    private final String address;
    private final String email;
    private final String paymentMethod;
    private final String note;
    private final String status;
    private final LocalDateTime createdAt;
    private final BigDecimal total;
    private final List<CustomerOrder> lines;
    // Da thu / con no (gan sau khi gom, tu bang payments)
    private BigDecimal paid;

    public BigDecimal getPaid() {
        return paid == null ? BigDecimal.ZERO : paid;
    }

    public void setPaid(BigDecimal paid) {
        this.paid = paid;
    }

    /** Con no = tong - da thu, khong am. Don huy khong tinh no. */
    public BigDecimal getDebt() {
        if ("CANCELED".equalsIgnoreCase(status)) {
            return BigDecimal.ZERO;
        }
        BigDecimal debt = (total == null ? BigDecimal.ZERO : total).subtract(getPaid());
        return debt.signum() < 0 ? BigDecimal.ZERO : debt;
    }

    public boolean isPaidInFull() {
        return getDebt().signum() == 0;
    }

    /** Cong no ke toan: chi tinh khi hang da giao (hoan thanh) ma chua thu du. */
    public BigDecimal getReceivable() {
        return "COMPLETED".equalsIgnoreCase(status) ? getDebt() : BigDecimal.ZERO;
    }

    public int getLineCount() {
        return lines == null ? 0 : lines.size();
    }
}
