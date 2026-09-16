package com.anhvu.vlxd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Danh gia cua khach hang hien tren trang chu.
 * Khach gui tu web -> approved = false, admin duyet moi hien. Admin tu nhap -> approved = true.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String customerName;

    /** Vai tro / khu vuc, vd "Thầu xây dựng · Bình Long". */
    @Column(length = 120)
    private String customerRole;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private boolean approved;

    /** WEB = khach tu gui, ADMIN = cua hang nhap lai tu Zalo / dien thoai. */
    @Column(length = 20)
    private String source;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
