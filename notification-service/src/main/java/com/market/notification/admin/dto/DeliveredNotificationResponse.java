package com.market.notification.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveredNotificationResponse(
        UUID id,
        UUID eventId,
        UUID orderId,
        String eventType,
        String channel,
        String message,
        Instant deliveredAt
) {
}
