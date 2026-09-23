package com.anhvu.vlxd.repository;

import com.anhvu.vlxd.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from CustomerOrder o where o.orderCode = :code order by o.id")
    List<CustomerOrder> findLockedByCode(@org.springframework.data.repository.query.Param("code") String code);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from CustomerOrder o where o.id = :id")
    java.util.Optional<CustomerOrder> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    List<CustomerOrder> findTop10ByOrderByCreatedAtDesc();

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    List<CustomerOrder> findByPhoneOrderByCreatedAtDesc(String phone);

    List<CustomerOrder> findByOrderCodeOrderByIdAsc(String orderCode);
}
