package com.anhvu.vlxd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mot lan thu tien cua mot don (theo orderCode). Cong no = tong don - tong thu. */
@Entity
@Table(name = "payments", indexes = @Index(name = "idx_payments_order", columnList = "orderCode"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String orderCode;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    // CASH / BANK_TRANSFER
    @Column(nullable = false, length = 30)
    private String method;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    @PrePersist
    void onCreate() {
        if (paidAt == null) {
            paidAt = LocalDateTime.now();
        }
    }
}
