package com.market.events.order;

import java.util.List;
import java.util.UUID;

public record OrderCancelledPayload(
        UUID orderId,
        String ownerId,
        String previousStatus,
        String reason,
        List<OrderCancelledLinePayload> items
) {
}
