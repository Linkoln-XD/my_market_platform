package com.market.notification.admin.dto;

import java.util.List;

public record NotificationPageResponse(
        List<DeliveredNotificationResponse> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}
