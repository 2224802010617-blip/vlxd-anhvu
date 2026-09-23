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
import java.util.Map;
import java.util.TreeMap;

/** Moi thay doi ton kho di qua day de co lich su nhap/xuat/dieu chinh. */
@Service
@RequiredArgsConstructor
public class InventoryService {

    public static final String IN = "IN";
    public static final String OUT = "OUT";
    public static final String ADJUST = "ADJUST";

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final InventoryPolicy inventoryPolicy;
    private final jakarta.persistence.EntityManager entityManager;

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
        Map<Long, Integer> outstanding = outstanding(orderCode);
        Map<Long, Integer> quantities = new TreeMap<>();
        for (CustomerOrder line : lines) {
            Product product = findProduct(line.getProductName()).orElseThrow(() ->
                    new IllegalStateException("Không tìm thấy sản phẩm: " + line.getProductName()));
            if (inventoryPolicy.isService(product)) continue;
            int qty;
            try {
                qty = line.getQuantity().setScale(0, RoundingMode.CEILING).intValueExact();
                if (qty <= 0) throw new ArithmeticException();
                quantities.merge(product.getId(), qty, Math::addExact);
            } catch (ArithmeticException | NullPointerException ex) {
                throw new IllegalStateException("Số lượng không hợp lệ: " + line.getProductName());
            }
        }
        Map<Long, Product> products = new TreeMap<>();
        // Lock in ID order so simultaneous orders cannot oversell shared products.
        for (var entry : quantities.entrySet()) {
            Product product = lockedProduct(entry.getKey());
            int needed = entry.getValue() - outstanding.getOrDefault(entry.getKey(), 0);
            if (needed < 0) throw new IllegalStateException("Lịch sử kho không khớp đơn " + orderCode);
            if (safeStock(product) < needed) {
                throw new IllegalStateException("Không đủ tồn kho: " + product.getName()
                        + " (còn " + safeStock(product) + ", cần " + needed + ").");
            }
            products.put(entry.getKey(), product);
        }
        for (var entry : quantities.entrySet()) {
            int needed = entry.getValue() - outstanding.getOrDefault(entry.getKey(), 0);
            if (needed > 0) changeStock(products.get(entry.getKey()), -needed, OUT, orderCode);
        }
    }

    /** Don roi khoi HOAN THANH (sua nham / huy): tra hang ve kho, cung chi mot lan. */
    @Transactional
    public void revertCompletion(String orderCode, List<CustomerOrder> lines) {
        for (var entry : outstanding(orderCode).entrySet()) {
            if (entry.getValue() > 0) {
                changeStock(lockedProduct(entry.getKey()), entry.getValue(), IN, "Hoàn " + orderCode);
            }
        }
    }

    private Map<Long, Integer> outstanding(String code) {
        Map<Long, Integer> result = new TreeMap<>();
        for (StockMovement movement : movementRepository.findByReferenceAndType(code, OUT)) {
            result.merge(movement.getProductId(), Math.negateExact(movement.getQuantity()), Math::addExact);
        }
        for (StockMovement movement : movementRepository.findByReferenceAndType("Hoàn " + code, IN)) {
            result.merge(movement.getProductId(), Math.negateExact(movement.getQuantity()), Math::addExact);
        }
        return result;
    }

    private Product lockedProduct(Long id) {
        Product product = productRepository.findLockedById(id).orElseThrow(() ->
                new IllegalStateException("Sản phẩm trong lịch sử kho không còn tồn tại: " + id));
        // Name resolution may have loaded an older snapshot before acquiring the lock.
        entityManager.refresh(product, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return product;
    }

    private void changeStock(Product product, int quantity, String type, String reference) {
        int after = Math.addExact(safeStock(product), quantity);
        product.setStockQuantity(after);
        productRepository.save(product);
        movementRepository.save(StockMovement.builder().productId(product.getId())
                .productName(product.getName()).type(type).quantity(quantity)
                .stockAfter(after).reference(reference).build());
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
