package com.anhvu.vlxd.service;

import com.anhvu.vlxd.entity.Product;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Nguong "sap het hang" theo don vi tinh: 260 m3 cat la nhieu, 260 vien gach la sap het.
 */
@Component
public class InventoryPolicy {

    public boolean isService(Product product) {
        String unit = normalize(product.getUnit());
        return unit.contains("bao gia") || (product.getPrice() != null && product.getPrice().signum() <= 0);
    }

    /** Tra ve -1 neu mat hang khong theo doi ton kho (dich vu). */
    public int lowStockThreshold(Product product) {
        if (isService(product)) {
            return -1;
        }
        String unit = normalize(product.getUnit());
        if (unit.startsWith("vien")) {
            return 2000;
        }
        if (unit.startsWith("bao")) {
            return 100;
        }
        if (unit.startsWith("kg")) {
            return 500;
        }
        if (unit.startsWith("tan")) {
            return 1;
        }
        if (unit.startsWith("m3") || unit.startsWith("khoi")) {
            return 30;
        }
        return 50;
    }

    public boolean isLowStock(Product product) {
        int threshold = lowStockThreshold(product);
        if (threshold < 0 || product.getStockQuantity() == null) {
            return false;
        }
        return product.getStockQuantity() <= threshold;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .trim();
    }
}
