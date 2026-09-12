package com.anhvu.vlxd.controller;

import com.anhvu.vlxd.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.TimeUnit;

/** Tra anh san pham luu trong DB tai /images/db/{productId}. */
@Controller
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @GetMapping("/images/db/{productId}")
    public ResponseEntity<byte[]> image(@PathVariable Long productId) {
        return productImageService.find(productId)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getContentType()))
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                        .body(image.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
