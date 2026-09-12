package com.anhvu.vlxd.repository;

import com.anhvu.vlxd.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop30ByOrderByCreatedAtDescIdDesc();

    List<StockMovement> findByProductIdOrderByCreatedAtDescIdDesc(Long productId);

    boolean existsByReferenceAndType(String reference, String type);
}
