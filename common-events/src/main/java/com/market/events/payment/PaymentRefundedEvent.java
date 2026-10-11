package com.market.events.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentRefundedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        PaymentRefundedPayload payload
) {
}
