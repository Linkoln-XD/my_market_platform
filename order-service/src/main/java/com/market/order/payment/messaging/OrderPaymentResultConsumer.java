package com.market.order.payment.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.payment.PaymentEventTypes;
import com.market.events.topics.PaymentTopics;
import com.market.order.messaging.ProcessedEventService;
import com.market.order.order.OrderStatus;
import com.market.order.order.domain.OrderEntity;
import com.market.order.order.repository.OrderRepository;
import com.market.order.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentResultConsumer {

    public static final String CONSUMER_NAME = "order-payment-result";

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;

    @KafkaListener(topics = PaymentTopics.PAYMENT_EVENTS, groupId = "order-payment-result")
    @Transactional
    public void onPaymentResult(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        String eventType = root.path("eventType").asText();
        if (PaymentEventTypes.PAYMENT_SUCCEEDED.equals(eventType)) {
            handleSucceeded(root);
        } else if (PaymentEventTypes.PAYMENT_FAILED.equals(eventType)) {
            handleFailed(root);
        }
    }

    private void handleSucceeded(JsonNode root) throws Exception {
        UUID eventId = UUID.fromString(root.path("eventId").asText());
        if (!processedEventService.markProcessedIfAbsent(CONSUMER_NAME, eventId)) {
            return;
        }

        JsonNode body = root.path("payload");
        UUID orderId = UUID.fromString(body.path("orderId").asText());
        UUID paymentId = UUID.fromString(body.path("paymentId").asText());
        long amountCents = body.path("amountCents").asLong();

        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for PaymentSucceeded", orderId);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.debug("Order {} already in status {}, skipping payment success", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        outboxService.enqueueOrderPaid(order, paymentId, amountCents);
        log.info("Order {} marked PAID after payment {}", orderId, paymentId);
    }

    private void handleFailed(JsonNode root) {
        UUID eventId = UUID.fromString(root.path("eventId").asText());
        if (!processedEventService.markProcessedIfAbsent(CONSUMER_NAME, eventId)) {
            return;
        }

        JsonNode body = root.path("payload");
        UUID orderId = UUID.fromString(body.path("orderId").asText());

        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.info("Order {} cancelled after failed payment", orderId);
            }
        });
    }
}
