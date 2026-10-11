package com.market.events.catalog;

import java.util.UUID;

public record StockReservedPayload(
        UUID orderId,
        UUID productId,
        int quantityReserved,
        int quantityOnHand
) {
}
