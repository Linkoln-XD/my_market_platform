package com.market.order.payment.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.order.OrderEventTypes;
import com.market.events.payment.PaymentEventTypes;
import com.market.events.payment.PaymentRequestedEvent;
import com.market.events.payment.PaymentRequestedPayload;
import com.market.events.topics.OrderTopics;
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
public class OrderCreatedPaymentConsumer {

    public static final String CONSUMER_NAME = "order-payment-initiation";

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = OrderTopics.ORDER_EVENTS, groupId = "order-payment-initiation")
    @Transactional
    public void onOrderEvent(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        if (!OrderEventTypes.ORDER_CREATED.equals(root.path("eventType").asText())) {
            return;
        }

        UUID eventId = UUID.fromString(root.path("eventId").asText());
        if (!processedEventService.markProcessedIfAbsent(CONSUMER_NAME, eventId)) {
            log.debug("Skip duplicate OrderCreated event {}", eventId);
            return;
        }

        JsonNode body = root.path("payload");
        UUID orderId = UUID.fromString(body.path("orderId").asText());
        long amountCents = body.path("subtotalCents").asLong();
        String currency = body.path("currency").asText();

        UUID paymentId = UUID.randomUUID();
        PaymentRequestedEvent requested = new PaymentRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                PaymentEventTypes.PAYMENT_REQUESTED,
                new PaymentRequestedPayload(paymentId, orderId, amountCents, currency));

        kafkaTemplate.send(
                PaymentTopics.PAYMENT_EVENTS,
                orderId.toString(),
                objectMapper.writeValueAsString(requested));
        log.info("Published PaymentRequested for order {} payment {}", orderId, paymentId);
    }
}
