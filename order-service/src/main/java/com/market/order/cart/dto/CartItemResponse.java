package com.market.order.cart.dto;

import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        String sku,
        String productName,
        long unitPriceCents,
        String currency,
        int quantity,
        long lineTotalCents
) {
}
