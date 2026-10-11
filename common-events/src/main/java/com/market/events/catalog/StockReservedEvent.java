package com.market.events.catalog;

import java.time.Instant;
import java.util.UUID;

public record StockReservedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        StockReservedPayload payload
) {
}
