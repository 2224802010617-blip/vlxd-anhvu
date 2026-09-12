package com.anhvu.vlxd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lich su kho: IN (nhap), OUT (xuat khi don hoan thanh), ADJUST (sua tay).
 * Luu ca ten san pham de lich su con doc duoc sau khi san pham bi xoa.
 */
@Entity
@Table(name = "stock_movements", indexes = @Index(name = "idx_stock_product", columnList = "productId"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(nullable = false, length = 10)
    private String type;

    // Duong = tang ton, am = giam ton
    @Column(nullable = false)
    private Integer quantity;

    private Integer stockAfter;

    // Gia von moi don vi khi nhap (null neu xuat/dieu chinh)
    @Column(precision = 14, scale = 2)
    private BigDecimal unitCost;

    // Ma don voi OUT, ghi chu tu do voi IN/ADJUST
    @Column(length = 255)
    private String reference;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
