package com.example.crudapi.dto;

import com.example.crudapi.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        CategorySummary category,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Phai goi trong pham vi transaction (hoac sau khi da fetch category), vi
     * Product.category la LAZY va open-in-view dang tat.
     */
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                CategorySummary.from(product.getCategory()),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
