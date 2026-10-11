package com.market.order.catalog.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.catalog.CatalogEventTypes;
import com.market.events.catalog.StockReservedEvent;
import com.market.events.catalog.StockReservedPayload;
import com.market.events.order.OrderEventTypes;
import com.market.events.topics.CatalogTopics;
import com.market.events.topics.OrderTopics;
import com.market.order.catalog.service.StockReservationService;
import com.market.order.messaging.ProcessedEventService;
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
public class OrderPaidStockReserveConsumer {

    public static final String CONSUMER_NAME = "catalog-stock-reserve";

    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;
    private final StockReservationService stockReservationService;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = OrderTopics.ORDER_EVENTS, groupId = "catalog-stock-reserve")
    @Transactional
    public void onOrderEvent(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        if (!OrderEventTypes.ORDER_PAID.equals(root.path("eventType").asText())) {
            return;
        }

        UUID eventId = UUID.fromString(root.path("eventId").asText());
        if (!processedEventService.markProcessedIfAbsent(CONSUMER_NAME, eventId)) {
            return;
        }

        UUID orderId = UUID.fromString(root.path("payload").path("orderId").asText());
        OrderEntity order = orderRepository.findWithItemsById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order {} not found for stock reservation", orderId);
            return;
        }

        for (var line : order.getItems()) {
            int onHand = stockReservationService.reserve(line.getProductId(), line.getQuantity());
            StockReservedEvent reserved = new StockReservedEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    CatalogEventTypes.STOCK_RESERVED,
                    new StockReservedPayload(orderId, line.getProductId(), line.getQuantity(), onHand));
            kafkaTemplate.send(
                    CatalogTopics.CATALOG_EVENTS,
                    line.getProductId().toString(),
                    objectMapper.writeValueAsString(reserved));
        }
        log.info("Reserved stock for paid order {}", orderId);
    }
}
