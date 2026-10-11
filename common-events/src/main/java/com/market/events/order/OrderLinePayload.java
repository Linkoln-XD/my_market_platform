package com.market.events.order;

import java.util.UUID;

public record OrderLinePayload(
        UUID productId,
        String sku,
        String productName,
        int quantity,
        long unitPriceCents,
        long lineTotalCents
) {
}
