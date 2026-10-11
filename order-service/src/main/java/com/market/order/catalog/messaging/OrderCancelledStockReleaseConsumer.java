package com.market.order.catalog.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.catalog.CatalogEventTypes;
import com.market.events.catalog.StockReleasedEvent;
import com.market.events.catalog.StockReleasedPayload;
import com.market.events.order.OrderEventTypes;
import com.market.events.topics.CatalogTopics;
import com.market.order.order.OrderStatus;
import com.market.events.topics.OrderTopics;
import com.market.order.catalog.service.StockReleaseService;
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
public class OrderCancelledStockReleaseConsumer {

    public static final String CONSUMER_NAME = "catalog-stock-release";

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final StockReleaseService stockReleaseService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = OrderTopics.ORDER_EVENTS, groupId = "catalog-stock-release")
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
            log.debug("Skip stock release for order {} cancelled from {}", 
                    body.path("orderId").asText(), body.path("previousStatus").asText());
            return;
        }

        UUID orderId = UUID.fromString(body.path("orderId").asText());
        for (JsonNode line : body.path("items")) {
            UUID productId = UUID.fromString(line.path("productId").asText());
            int quantity = line.path("quantity").asInt();
            int onHand = stockReleaseService.release(productId, quantity);
            StockReleasedEvent released = new StockReleasedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    CatalogEventTypes.STOCK_RELEASED,
                    new StockReleasedPayload(orderId, productId, quantity, onHand));
            kafkaTemplate.send(
                    CatalogTopics.CATALOG_EVENTS,
                    productId.toString(),
                    objectMapper.writeValueAsString(released));
        }
        log.info("Released stock for cancelled paid order {}", orderId);
    }
}
