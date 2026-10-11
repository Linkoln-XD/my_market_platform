package com.market.notification.admin;

import com.market.notification.admin.dto.DeliveredNotificationResponse;
import com.market.notification.admin.dto.NotificationPageResponse;
import com.market.notification.domain.DeliveredNotificationEntity;
import com.market.notification.repository.DeliveredNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final DeliveredNotificationRepository deliveredNotificationRepository;

    @Transactional(readOnly = true)
    public NotificationPageResponse list(Pageable pageable) {
        Page<DeliveredNotificationEntity> page =
                deliveredNotificationRepository.findAllByOrderByDeliveredAtDesc(pageable);
        return new NotificationPageResponse(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    private DeliveredNotificationResponse toResponse(DeliveredNotificationEntity entity) {
        return new DeliveredNotificationResponse(
                entity.getId(),
                entity.getEventId(),
                entity.getOrderId(),
                entity.getEventType(),
                entity.getChannel(),
                entity.getMessage(),
                entity.getDeliveredAt());
    }
}
