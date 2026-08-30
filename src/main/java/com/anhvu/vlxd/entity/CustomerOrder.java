package com.anhvu.vlxd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 180)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    // Gia ban tai thoi diem dat (luu lai de doanh thu khong sai khi gia doi ve sau)
    @Column(precision = 14, scale = 2)
    private BigDecimal unitPrice;

    // Thanh tien = don gia * so luong tai thoi diem dat
    @Column(precision = 16, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 40)
    private String paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null || status.isBlank()) {
            status = "NEW";
        }
    }
}
