package com.market.events.order;

import java.util.List;
import java.util.UUID;

public record OrderCreatedPayload(
        UUID orderId,
        String ownerId,
        String status,
        String currency,
        long subtotalCents,
        List<OrderLinePayload> items
) {
}
