package com.anhvu.vlxd.service.impl;

import com.anhvu.vlxd.entity.Category;
import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.repository.ProductRepository;
import com.anhvu.vlxd.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        return searchProducts(keyword, null);
    }

    @Override
    public List<Product> searchProducts(String keyword, String category) {
        if (category != null && !category.isBlank()) {
            List<String> categoryNames = switch (category.toLowerCase(Locale.ROOT)) {
                case "gach" -> List.of("gạch");
                case "xi-mang" -> List.of("xi măng");
                case "cat-da" -> List.of("cát xây dựng", "đá xây dựng");
                case "thep" -> List.of("thép");
                case "dich-vu" -> List.of("dịch vụ");
                default -> List.of();
            };

            if (!categoryNames.isEmpty()) {
                String normalizedKeyword = keyword == null ? "" : keyword.trim();
                return productRepository.searchActiveProductsByCategory(normalizedKeyword, categoryNames);
            }
        }

        if (keyword == null || keyword.isBlank()) {
            return getAllActiveProducts();
        }
        return productRepository.searchActiveProducts(keyword.trim());
    }

    @Override
    public BigDecimal calculateMaterialQuantity(BigDecimal areaM2, BigDecimal consumptionPerM2) {
        if (areaM2 == null || consumptionPerM2 == null) {
            return BigDecimal.ZERO;
        }
        return areaM2.multiply(consumptionPerM2).setScale(2, RoundingMode.UP);
    }

    @Override
    public Product getActiveProductById(Long id) {
        if (id == null) {
            return null;
        }
        return productRepository.findById(id)
                .filter(Product::getActive)
                .orElse(null);
    }

    @Override
    public List<Product> getRelatedProducts(Product product, int limit) {
        if (product == null || product.getCategory() == null) {
            return List.of();
        }
        List<Product> related = productRepository
                .findTop8ByCategoryAndActiveTrueAndIdNotOrderByStockQuantityDesc(product.getCategory(), product.getId());
        if (related.size() > limit) {
            return related.subList(0, limit);
        }
        return related;
    }
}
