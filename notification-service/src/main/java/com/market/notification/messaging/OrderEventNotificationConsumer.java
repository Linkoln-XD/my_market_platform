package com.market.notification.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.topics.OrderTopics;
import com.market.notification.config.NotificationProperties;
import com.market.notification.service.NotificationDispatchService;
import com.market.notification.service.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Subscribes only to {@link OrderTopics#ORDER_EVENTS}. No HTTP calls to order-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final NotificationDispatchService notificationDispatchService;
    private final NotificationProperties notificationProperties;

    @KafkaListener(
            topics = OrderTopics.ORDER_EVENTS,
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onOrderEvent(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        String eventType = root.path("eventType").asText(null);
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Missing eventType in payload");
        }

        UUID eventId = UUID.fromString(root.path("eventId").asText());
        MDC.put("eventId", eventId.toString());
        try {
            if (!processedEventService.markProcessedIfAbsent(notificationProperties.consumerName(), eventId)) {
                log.debug("Skip duplicate notification for event {}", eventId);
                return;
            }

            UUID orderId = UUID.fromString(root.path("payload").path("orderId").asText());
            notificationDispatchService.dispatch(eventId, orderId, eventType);
        } finally {
            MDC.remove("eventId");
        }
    }
}
