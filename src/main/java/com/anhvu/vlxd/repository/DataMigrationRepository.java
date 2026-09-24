package com.anhvu.vlxd.repository;

import com.anhvu.vlxd.entity.DataMigration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataMigrationRepository extends JpaRepository<DataMigration, String> {
}
