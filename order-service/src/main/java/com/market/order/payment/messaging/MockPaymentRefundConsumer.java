package com.market.order.payment.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.payment.PaymentEventTypes;
import com.market.events.payment.PaymentRefundedEvent;
import com.market.events.payment.PaymentRefundedPayload;
import com.market.events.topics.PaymentTopics;
import com.market.order.messaging.ProcessedEventService;
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
public class MockPaymentRefundConsumer {

    public static final String CONSUMER_NAME = "mock-payment-refund";

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = PaymentTopics.PAYMENT_EVENTS, groupId = "mock-payment-refund")
    @Transactional
    public void onPaymentEvent(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        if (!PaymentEventTypes.PAYMENT_REFUND_REQUESTED.equals(root.path("eventType").asText())) {
            return;
        }

        UUID eventId = UUID.fromString(root.path("eventId").asText());
        if (!processedEventService.markProcessedIfAbsent(CONSUMER_NAME, eventId)) {
            return;
        }

        JsonNode body = root.path("payload");
        UUID orderId = UUID.fromString(body.path("orderId").asText());
        PaymentRefundedEvent refunded = new PaymentRefundedEvent(
                UUID.randomUUID(),
                Instant.now(),
                PaymentEventTypes.PAYMENT_REFUNDED,
                new PaymentRefundedPayload(
                        UUID.fromString(body.path("refundId").asText()),
                        orderId,
                        body.path("amountCents").asLong(),
                        body.path("currency").asText()));

        kafkaTemplate.send(
                PaymentTopics.PAYMENT_EVENTS,
                orderId.toString(),
                objectMapper.writeValueAsString(refunded));
        log.info("Mock refund completed for order {}", orderId);
    }
}
