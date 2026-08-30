package com.anhvu.vlxd.repository;

import com.anhvu.vlxd.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findTop10ByOrderByCreatedAtDesc();

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    List<CustomerOrder> findByPhoneOrderByCreatedAtDesc(String phone);
}
