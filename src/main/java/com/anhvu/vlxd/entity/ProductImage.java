package com.anhvu.vlxd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Anh san pham do admin tai len. Luu trong DB vi file he thong tren Railway
 * bi xoa moi lan deploy. Tach bang rieng de findAll() cua Product khong keo theo du lieu anh.
 */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    private Long productId;

    @Column(nullable = false, length = 60)
    private String contentType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    private LocalDateTime updatedAt;
}
