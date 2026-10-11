package com.market.order.payment.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.payment.PaymentEventTypes;
import com.market.events.payment.PaymentRefundRequestedEvent;
import com.market.events.payment.PaymentRefundRequestedPayload;
import com.market.events.order.OrderEventTypes;
import com.market.events.topics.OrderTopics;
import com.market.events.topics.PaymentTopics;
import com.market.order.messaging.ProcessedEventService;
import com.market.order.order.OrderStatus;
import com.market.order.order.domain.OrderEntity;
import com.market.order.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledRefundConsumer {

    public static final String CONSUMER_NAME = "order-cancel-refund";

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = OrderTopics.ORDER_EVENTS, groupId = "order-cancel-refund")
    @Transactional
    public void onOrderEvent(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        if (!OrderEventTypes.ORDER_CANCELLED.equals(root.path("eventType").asText())) {
            return;
        }

        UUID eventId = UUID.fromString(root.path("eventId").asText());
        if (!processedEventService.markProcessedIfAbsent(CONSUMER_NAME, eventId)) {
            return;
        }

        JsonNode body = root.path("payload");
        if (!OrderStatus.PAID.toString().equals(body.path("previousStatus").asText())) {
            return;
        }

        UUID orderId = UUID.fromString(body.path("orderId").asText());
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }

        PaymentRefundRequestedEvent refundRequested = new PaymentRefundRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                PaymentEventTypes.PAYMENT_REFUND_REQUESTED,
                new PaymentRefundRequestedPayload(
                        UUID.randomUUID(),
                        orderId,
                        order.getSubtotalCents(),
                        order.getCurrency(),
                        body.path("reason").asText()));

        kafkaTemplate.send(
                PaymentTopics.PAYMENT_EVENTS,
                orderId.toString(),
                objectMapper.writeValueAsString(refundRequested));
        log.info("Requested refund for cancelled order {}", orderId);
    }
}
