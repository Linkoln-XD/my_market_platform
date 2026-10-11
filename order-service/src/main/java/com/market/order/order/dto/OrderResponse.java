package com.market.order.order.dto;

import com.market.order.order.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String ownerId,
        OrderStatus status,
        String currency,
        long subtotalCents,
        List<OrderItemResponse> items,
        Instant createdAt
) {
}
