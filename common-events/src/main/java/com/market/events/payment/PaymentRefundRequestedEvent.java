package com.market.events.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentRefundRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        PaymentRefundRequestedPayload payload
) {
}
