package com.market.events.order;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        OrderCancelledPayload payload
) {
}
