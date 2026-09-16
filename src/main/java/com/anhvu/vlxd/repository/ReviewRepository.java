package com.anhvu.vlxd.repository;

import com.anhvu.vlxd.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByApprovedTrueOrderByCreatedAtDesc();

    List<Review> findAllByOrderByCreatedAtDesc();

    long countByApprovedFalse();
}
