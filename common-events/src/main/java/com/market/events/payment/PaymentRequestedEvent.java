package com.market.events.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        PaymentRequestedPayload payload
) {
}
