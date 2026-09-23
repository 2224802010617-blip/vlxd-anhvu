package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.repository.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderStatusService {
    private final CustomerOrderRepository repository;
    private final InventoryService inventory;

    @Transactional
    public List<CustomerOrder> update(String code, String status) {
        if (!AdminReportService.ORDER_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trang thai khong hop le");
        }
        List<CustomerOrder> lines = new ArrayList<>(repository.findLockedByCode(code));
        if (lines.isEmpty() && code.startsWith("DH")) {
            try {
                repository.findLockedById(Long.parseLong(code.substring(2)))
                        .filter(line -> line.getOrderCode() == null || line.getOrderCode().isBlank())
                        .ifPresent(lines::add);
            } catch (NumberFormatException ignored) {
                // Legacy orders use DH followed by the row ID.
            }
        }
        if (lines.isEmpty()) throw new IllegalArgumentException("Khong tim thay don hang " + code);
        boolean completed = "COMPLETED".equals(lines.get(0).getStatus());
        if ("COMPLETED".equals(status) && !completed) inventory.applyCompletion(code, lines);
        if (!"COMPLETED".equals(status) && completed) inventory.revertCompletion(code, lines);
        lines.forEach(line -> line.setStatus(status));
        return repository.saveAll(lines);
    }
}
