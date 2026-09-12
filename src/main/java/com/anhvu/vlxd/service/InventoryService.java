package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.CustomerOrder;
import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.entity.StockMovement;
import com.anhvu.vlxd.repository.ProductRepository;
import com.anhvu.vlxd.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Moi thay doi ton kho di qua day de co lich su nhap/xuat/dieu chinh. */
@Service
@RequiredArgsConstructor
public class InventoryService {

    public static final String IN = "IN";
    public static final String OUT = "OUT";
    public static final String ADJUST = "ADJUST";

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    /** Nhap kho: tang ton, ghi gia von moi nhat len san pham. */
    @Transactional
    public void receive(Product product, int quantity, BigDecimal unitCost, String note) {
        int after = safeStock(product) + quantity;
        product.setStockQuantity(after);
        if (unitCost != null && unitCost.signum() > 0) {
            product.setCostPrice(unitCost.setScale(2, RoundingMode.HALF_UP));
        }
        productRepository.save(product);
        movementRepository.save(StockMovement.builder()
                .productId(product.getId())
                .productName(product.getName())
                .type(IN)
                .quantity(quantity)
                .stockAfter(after)
                .unitCost(unitCost)
                .reference(note == null || note.isBlank() ? "Nhập kho" : note.trim())
                .build());
    }

    /** Sua tay so ton (tu bang kho): chi ghi lich su neu co thay doi. */
    @Transactional
    public void adjust(Product product, int newStock, String note) {
        int before = safeStock(product);
        if (before == newStock) {
            return;
        }
        product.setStockQuantity(newStock);
        productRepository.save(product);
        movementRepository.save(StockMovement.builder()
                .productId(product.getId())
                .productName(product.getName())
                .type(ADJUST)
                .quantity(newStock - before)
                .stockAfter(newStock)
                .reference(note == null || note.isBlank() ? "Sửa tay trong bảng kho" : note.trim())
                .build());
    }

    /**
     * Don chuyen sang HOAN THANH: tru ton tung dong hang (moi don chi tru mot lan).
     * Ton kho la so nguyen; so luong don co the le (m3) -> lam tron len.
     */
    @Transactional
    public void applyCompletion(String orderCode, List<CustomerOrder> lines) {
        if (movementRepository.existsByReferenceAndType(orderCode, OUT)) {
            return;
        }
        for (CustomerOrder line : lines) {
            findProduct(line.getProductName()).ifPresent(product -> {
                int qty = line.getQuantity() == null ? 0 : line.getQuantity().setScale(0, RoundingMode.CEILING).intValue();
                if (qty <= 0) {
                    return;
                }
                int after = Math.max(0, safeStock(product) - qty);
                product.setStockQuantity(after);
                productRepository.save(product);
                movementRepository.save(StockMovement.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .type(OUT)
                        .quantity(-qty)
                        .stockAfter(after)
                        .reference(orderCode)
                        .build());
            });
        }
    }

    /** Don roi khoi HOAN THANH (sua nham / huy): tra hang ve kho, cung chi mot lan. */
    @Transactional
    public void revertCompletion(String orderCode, List<CustomerOrder> lines) {
        String reference = "Hoàn " + orderCode;
        if (!movementRepository.existsByReferenceAndType(orderCode, OUT)
                || movementRepository.existsByReferenceAndType(reference, IN)) {
            return;
        }
        for (CustomerOrder line : lines) {
            findProduct(line.getProductName()).ifPresent(product -> {
                int qty = line.getQuantity() == null ? 0 : line.getQuantity().setScale(0, RoundingMode.CEILING).intValue();
                if (qty <= 0) {
                    return;
                }
                int after = safeStock(product) + qty;
                product.setStockQuantity(after);
                productRepository.save(product);
                movementRepository.save(StockMovement.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .type(IN)
                        .quantity(qty)
                        .stockAfter(after)
                        .reference(reference)
                        .build());
            });
        }
    }

    public List<StockMovement> recent() {
        return movementRepository.findTop30ByOrderByCreatedAtDescIdDesc();
    }

    private Optional<Product> findProduct(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        return productRepository.findAll().stream()
                .filter(product -> product.getName() != null && product.getName().trim().toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst();
    }

    private static int safeStock(Product product) {
        return product.getStockQuantity() == null ? 0 : product.getStockQuantity();
    }
}
