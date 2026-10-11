package com.market.notification.service;

import com.market.notification.domain.DeliveredNotificationEntity;
import com.market.notification.repository.DeliveredNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    private static final Set<String> NOTIFY_EVENT_TYPES = Set.of(
            "OrderCreated",
            "OrderPaid",
            "OrderCancelled");

    private final DeliveredNotificationRepository deliveredNotificationRepository;

    @Transactional
    public void dispatch(UUID eventId, UUID orderId, String eventType) {
        if (!NOTIFY_EVENT_TYPES.contains(eventType)) {
            return;
        }

        String logMessage = "NOTIFY [%s] order=%s event=%s".formatted(eventType, orderId, eventId);
        log.info(logMessage);

        String emailStub = "EMAIL_STUB to owner of order %s: %s".formatted(orderId, eventType);
        log.info(emailStub);

        DeliveredNotificationEntity row = new DeliveredNotificationEntity();
        row.setEventId(eventId);
        row.setOrderId(orderId);
        row.setEventType(eventType);
        row.setChannel("LOG_EMAIL_STUB");
        row.setMessage(logMessage + " | " + emailStub);
        deliveredNotificationRepository.save(row);
    }
}
