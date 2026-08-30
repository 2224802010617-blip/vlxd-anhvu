package com.anhvu.vlxd.repository;

import com.anhvu.vlxd.entity.QuoteRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
    List<QuoteRequest> findTop10ByOrderByCreatedAtDesc();

    List<QuoteRequest> findAllByOrderByCreatedAtDesc();
}
