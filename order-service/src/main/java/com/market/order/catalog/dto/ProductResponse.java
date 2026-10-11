package com.market.order.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String sku,
        String name,
        String description,
        long priceCents,
        String currency,
        boolean active,
        int stockQuantity,
        Instant createdAt,
        Instant updatedAt
) {
}
