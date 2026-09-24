package com.anhvu.vlxd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Danh dau cac lan bo sung du lieu da chay tren DB nay, de moi buoc chi chay mot lan
 * (admin xoa san pham thi khoi dong lai khong bi tao lai).
 */
@Entity
@Table(name = "data_migrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DataMigration {

    @Id
    @Column(length = 100)
    private String id;

    private LocalDateTime appliedAt;
}
