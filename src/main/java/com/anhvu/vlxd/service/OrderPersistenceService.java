package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.repository.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPersistenceService {
    private final CustomerOrderRepository repository;

    @Transactional
    public List<CustomerOrder> create(List<CustomerOrder> lines) {
        if (lines.isEmpty()) throw new IllegalArgumentException("Don hang rong");
        List<CustomerOrder> saved = repository.saveAll(lines);
        String code = "DH" + saved.get(0).getId();
        saved.forEach(line -> line.setOrderCode(code));
        return repository.saveAll(saved);
    }
}
