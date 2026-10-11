package com.market.events.catalog;

import java.time.Instant;
import java.util.UUID;

public record StockReleasedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        StockReleasedPayload payload
) {
}
