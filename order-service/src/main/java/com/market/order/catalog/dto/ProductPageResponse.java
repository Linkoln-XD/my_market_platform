package com.market.order.catalog.dto;

import java.util.List;

/** JSON-friendly page wrapper for Redis cache (Spring Data {@code Page} is awkward to deserialize). */
public record ProductPageResponse(
        List<ProductResponse> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}
