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
    private final String paymentMethod;
    private final String note;
    private final String status;
    private final LocalDateTime createdAt;
    private final BigDecimal total;
    private final List<CustomerOrder> lines;

    public int getLineCount() {
        return lines == null ? 0 : lines.size();
    }
}
