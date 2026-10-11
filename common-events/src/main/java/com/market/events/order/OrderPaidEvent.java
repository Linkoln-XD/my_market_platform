package com.market.events.order;

import java.time.Instant;
import java.util.UUID;

public record OrderPaidEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        OrderPaidPayload payload
) {
}
