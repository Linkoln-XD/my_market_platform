package com.market.order.order.dto;

import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String sku,
        String productName,
        int quantity,
        long unitPriceCents,
        long lineTotalCents
) {
}
