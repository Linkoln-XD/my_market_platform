package com.market.order.cart.dto;

import java.time.Instant;
import java.util.List;

public record CartResponse(
        String ownerId,
        List<CartItemResponse> items,
        int totalQuantity,
        long subtotalCents,
        String currency,
        Instant updatedAt
) {
}
