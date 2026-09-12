package com.anhvu.vlxd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Danh ba email khach: gom tu don hang va yeu cau bao gia.
 * marketingConsent = khach tu tick dong y nhan tin khuyen mai (Nghi dinh 91/2020 yeu cau dong y truoc).
 * unsubscribed = khach bam huy nhan trong email, uu tien hon consent.
 */
@Entity
@Table(name = "email_contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailContact {

    @Id
    @Column(length = 160)
    private String email;

    @Column(length = 120)
    private String name;

    @Column(nullable = false)
    private boolean marketingConsent;

    @Column(nullable = false)
    private boolean unsubscribed;

    /** Ma ngau nhien trong link huy nhan, de khach khong huy nham nguoi khac. */
    @Column(nullable = false, length = 40)
    private String token;

    @Column(length = 30)
    private String source;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Chi gui tin khuyen mai khi khach dong y va chua huy. */
    public boolean canReceiveMarketing() {
        return marketingConsent && !unsubscribed;
    }
}
