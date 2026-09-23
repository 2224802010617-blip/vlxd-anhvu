package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.*;
import com.anhvu.vlxd.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.config.import=", "spring.jpa.show-sql=false"}, showSql = false)
@Import({OrderStatusService.class, OrderPersistenceService.class, InventoryService.class, InventoryPolicy.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderInventoryIntegrationTest {
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired CustomerOrderRepository orders;
    @Autowired StockMovementRepository movements;
    @Autowired OrderStatusService statuses;
    @Autowired OrderPersistenceService persistence;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private Product product;

    @BeforeEach void setup() {
        movements.deleteAll();
        orders.deleteAll();
        products.deleteAll();
        categories.deleteAll();
        Category category = categories.save(Category.builder().name("Test materials").build());
        product = products.save(Product.builder().name("Brick").category(category)
                .price(BigDecimal.TEN).stockQuantity(10).unit("vien").active(true).build());
    }

    private CustomerOrder line(int quantity) {
        return CustomerOrder.builder().customerName("Test").phone("0900000000").address("Test address")
                .productName(product.getName()).quantity(BigDecimal.valueOf(quantity)).status("NEW").build();
    }
    private String order(int quantity) {
        return persistence.create(List.of(line(quantity))).get(0).getOrderCode();
    }
    private int stock() { return products.findById(product.getId()).orElseThrow().getStockQuantity(); }

    @Test void repeatedCompletionAndReversalRemainBalanced() {
        String code = order(3);
        for (int i = 0; i < 3; i++) {
            statuses.update(code, "COMPLETED");
            statuses.update(code, "COMPLETED");
            assertThat(stock()).isEqualTo(7);
            statuses.update(code, "CONFIRMED");
            statuses.update(code, "CONFIRMED");
            assertThat(stock()).isEqualTo(10);
        }
        assertThat(movements.count()).isEqualTo(6);
    }

    @Test void insufficientStockRollsBackStatusAndAllMovements() {
        String code = persistence.create(List.of(line(6), line(5))).get(0).getOrderCode();
        assertThatThrownBy(() -> statuses.update(code, "COMPLETED")).isInstanceOf(IllegalStateException.class);
        assertThat(stock()).isEqualTo(10);
        assertThat(movements.count()).isZero();
        assertThat(orders.findByOrderCodeOrderByIdAsc(code)).allMatch(o -> "NEW".equals(o.getStatus()));
    }

    @Test void reversalUsesRecordedProductIdEvenAfterRename() {
        String code = order(3);
        statuses.update(code, "COMPLETED");
        Product renamed = products.findById(product.getId()).orElseThrow();
        renamed.setName("Renamed brick");
        products.save(renamed);
        statuses.update(code, "CANCELED");
        assertThat(stock()).isEqualTo(10);
    }

    @Test void invalidSecondLineRollsBackEntireOrder() {
        CustomerOrder invalid = line(2);
        invalid.setAddress(null);
        assertThatThrownBy(() -> persistence.create(List.of(line(1), invalid))).isInstanceOf(RuntimeException.class);
        assertThat(orders.count()).isZero();
    }

    @Test void statusWriteFailureAlsoRollsBackInventory() {
        String code = order(3);
        jdbc.execute("alter table customer_orders add constraint test_status_failure check (status <> 'COMPLETED')");
        try {
            assertThatThrownBy(() -> statuses.update(code, "COMPLETED")).isInstanceOf(RuntimeException.class);
            assertThat(stock()).isEqualTo(10);
            assertThat(movements.count()).isZero();
            assertThat(orders.findByOrderCodeOrderByIdAsc(code).get(0).getStatus()).isEqualTo("NEW");
        } finally {
            jdbc.execute("alter table customer_orders drop constraint test_status_failure");
        }
    }

    @Test void multipleLinesShareOneCode() {
        List<CustomerOrder> saved = persistence.create(List.of(line(2), line(3)));
        assertThat(orders.findAll()).hasSize(2).allMatch(o -> saved.get(0).getOrderCode().equals(o.getOrderCode()));
        statuses.update(saved.get(0).getOrderCode(), "COMPLETED");
        assertThat(stock()).isEqualTo(5);
    }

    @Test void concurrentOrdersCannotOversell() throws Exception {
        String first = order(7);
        String second = order(7);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = List.of(first, second).stream().map(code -> executor.submit(() -> {
                start.await();
                try { statuses.update(code, "COMPLETED"); return true; }
                catch (IllegalStateException ex) { return false; }
            })).toList();
            start.countDown();
            int completed = 0;
            for (Future<Boolean> result : results) if (result.get(20, TimeUnit.SECONDS)) completed++;
            assertThat(completed).isEqualTo(1);
            assertThat(stock()).isEqualTo(3);
            assertThat(movements.count()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }
}
