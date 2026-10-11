package com.market.events.catalog;

import java.util.UUID;

public record StockReleasedPayload(
        UUID orderId,
        UUID productId,
        int quantityReleased,
        int quantityOnHand
) {
}
