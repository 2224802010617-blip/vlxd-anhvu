package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    List<Product> getAllActiveProducts();

    List<Product> searchProducts(String keyword);

    List<Product> searchProducts(String keyword, String category);

    Product getActiveProductById(Long id);

    List<Product> getRelatedProducts(Product product, int limit);

    BigDecimal calculateMaterialQuantity(BigDecimal areaM2, BigDecimal consumptionPerM2);
}
