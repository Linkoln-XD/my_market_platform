package com.market.events.order;

import java.util.UUID;

public record OrderCancelledLinePayload(
        UUID productId,
        String sku,
        int quantity
) {
}
