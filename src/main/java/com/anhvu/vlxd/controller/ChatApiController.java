package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.entity.Product;
import com.anhvu.vlxd.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ChatApiController {

    private final ProductService productService;

    @GetMapping("/api/chat/products")
    public List<Map<String, Object>> getProductsForChat() {
        return productService.getAllActiveProducts().stream()
                .map(product -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", product.getName());
                    map.put("price", product.getPrice());
                    map.put("unit", product.getUnit());
                    map.put("stock", product.getStockQuantity());
                    map.put("category", product.getCategory().getName());
                    map.put("consumption", product.getConsumptionPerM2());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
