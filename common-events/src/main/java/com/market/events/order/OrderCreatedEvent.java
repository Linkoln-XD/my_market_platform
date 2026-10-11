package com.market.events.order;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@link com.market.events.topics.OrderTopics#ORDER_EVENTS} when an order is persisted.
 */
public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        OrderCreatedPayload payload
) {
}
