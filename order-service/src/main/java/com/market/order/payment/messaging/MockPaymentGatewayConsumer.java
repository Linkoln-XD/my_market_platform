package com.market.order.payment.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.payment.PaymentEventTypes;
import com.market.events.payment.PaymentFailedEvent;
import com.market.events.payment.PaymentFailedPayload;
import com.market.events.payment.PaymentSucceededEvent;
import com.market.events.payment.PaymentSucceededPayload;
import com.market.events.topics.PaymentTopics;
import com.market.order.messaging.ProcessedEventService;
import com.market.order.payment.PaymentMockProperties;
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
public class MockPaymentGatewayConsumer {

    public static final String CONSUMER_NAME = "mock-payment-gateway";

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentMockProperties paymentMockProperties;

    @KafkaListener(topics = PaymentTopics.PAYMENT_EVENTS, groupId = "mock-payment-gateway")
    @Transactional
    public void onPaymentEvent(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        if (!PaymentEventTypes.PAYMENT_REQUESTED.equals(root.path("eventType").asText())) {
            return;
        }

        UUID eventId = UUID.fromString(root.path("eventId").asText());
        if (!processedEventService.markProcessedIfAbsent(CONSUMER_NAME, eventId)) {
            log.debug("Skip duplicate PaymentRequested event {}", eventId);
            return;
        }

        JsonNode body = root.path("payload");
        UUID paymentId = UUID.fromString(body.path("paymentId").asText());
        UUID orderId = UUID.fromString(body.path("orderId").asText());
        long amountCents = body.path("amountCents").asLong();
        String currency = body.path("currency").asText();

        if (paymentMockProperties.forceDeclineOrderId() != null
                && paymentMockProperties.forceDeclineOrderId().equals(orderId)) {
            PaymentFailedEvent failed = new PaymentFailedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    PaymentEventTypes.PAYMENT_FAILED,
                    new PaymentFailedPayload(
                            paymentId, orderId, amountCents, currency, "Mock gateway declined payment"));
            kafkaTemplate.send(
                    PaymentTopics.PAYMENT_EVENTS,
                    orderId.toString(),
                    objectMapper.writeValueAsString(failed));
            log.info("Mock gateway declined payment for order {}", orderId);
            return;
        }

        PaymentSucceededEvent succeeded = new PaymentSucceededEvent(
                UUID.randomUUID(),
                Instant.now(),
                PaymentEventTypes.PAYMENT_SUCCEEDED,
                new PaymentSucceededPayload(paymentId, orderId, amountCents, currency));
        kafkaTemplate.send(
                PaymentTopics.PAYMENT_EVENTS,
                orderId.toString(),
                objectMapper.writeValueAsString(succeeded));
        log.info("Mock gateway succeeded payment for order {}", orderId);
    }
}
