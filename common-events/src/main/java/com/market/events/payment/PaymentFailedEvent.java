package com.market.events.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        PaymentFailedPayload payload
) {
}
