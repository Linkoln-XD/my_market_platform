package com.market.events.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        PaymentSucceededPayload payload
) {
}
