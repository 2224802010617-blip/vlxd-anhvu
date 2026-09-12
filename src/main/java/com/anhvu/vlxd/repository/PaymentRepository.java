package com.anhvu.vlxd.repository;

import com.anhvu.vlxd.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderCodeOrderByPaidAtAsc(String orderCode);

    void deleteByOrderCode(String orderCode);
}
