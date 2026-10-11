package com.market.order.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.order.OrderCancelledEvent;
import com.market.events.order.OrderCancelledLinePayload;
import com.market.events.order.OrderCancelledPayload;
import com.market.events.order.OrderCreatedEvent;
import com.market.events.order.OrderCreatedPayload;
import com.market.events.order.OrderEventTypes;
import com.market.events.order.OrderLinePayload;
import com.market.events.order.OrderPaidEvent;
import com.market.events.order.OrderPaidPayload;
import com.market.events.topics.OrderTopics;
import com.market.order.order.domain.OrderEntity;
import com.market.order.order.domain.OrderItemEntity;
import com.market.order.outbox.domain.OutboxEventEntity;
import com.market.order.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes outbox rows in the same database transaction as domain changes (see {@code OrderService}).
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String AGGREGATE_ORDER = "Order";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void enqueueOrderCreated(OrderEntity order) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        OrderCreatedPayload payload = new OrderCreatedPayload(
                order.getId(),
                order.getOwnerId(),
                order.getStatus().name(),
                order.getCurrency(),
                order.getSubtotalCents(),
                order.getItems().stream().map(this::toLinePayload).toList());

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId, occurredAt, OrderEventTypes.ORDER_CREATED, payload);

        OutboxEventEntity row = new OutboxEventEntity();
        row.setAggregateType(AGGREGATE_ORDER);
        row.setAggregateId(order.getId());
        row.setEventType(OrderEventTypes.ORDER_CREATED);
        row.setTopic(OrderTopics.ORDER_EVENTS);
        row.setEventId(eventId);
        row.setOccurredAt(occurredAt);
        row.setPayload(writeJson(event));
        outboxEventRepository.save(row);
    }

    public void enqueueOrderPaid(OrderEntity order, UUID paymentId, long amountCents) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        OrderPaidPayload payload = new OrderPaidPayload(
                order.getId(),
                paymentId,
                amountCents,
                order.getCurrency(),
                order.getStatus().name());

        OrderPaidEvent event = new OrderPaidEvent(
                eventId, occurredAt, OrderEventTypes.ORDER_PAID, payload);

        OutboxEventEntity row = new OutboxEventEntity();
        row.setAggregateType(AGGREGATE_ORDER);
        row.setAggregateId(order.getId());
        row.setEventType(OrderEventTypes.ORDER_PAID);
        row.setTopic(OrderTopics.ORDER_EVENTS);
        row.setEventId(eventId);
        row.setOccurredAt(occurredAt);
        row.setPayload(writeJson(event));
        outboxEventRepository.save(row);
    }

    public void enqueueOrderCancelled(OrderEntity order, String previousStatus, String reason) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        OrderCancelledPayload payload = new OrderCancelledPayload(
                order.getId(),
                order.getOwnerId(),
                previousStatus,
                reason,
                order.getItems().stream()
                        .map(item -> new OrderCancelledLinePayload(
                                item.getProductId(), item.getSku(), item.getQuantity()))
                        .toList());

        OrderCancelledEvent event = new OrderCancelledEvent(
                eventId, occurredAt, OrderEventTypes.ORDER_CANCELLED, payload);

        OutboxEventEntity row = new OutboxEventEntity();
        row.setAggregateType(AGGREGATE_ORDER);
        row.setAggregateId(order.getId());
        row.setEventType(OrderEventTypes.ORDER_CANCELLED);
        row.setTopic(OrderTopics.ORDER_EVENTS);
        row.setEventId(eventId);
        row.setOccurredAt(occurredAt);
        row.setPayload(writeJson(event));
        outboxEventRepository.save(row);
    }

    private OrderLinePayload toLinePayload(OrderItemEntity item) {
        return new OrderLinePayload(
                item.getProductId(),
                item.getSku(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPriceCents(),
                item.getLineTotalCents());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize outbox payload", ex);
        }
    }
}
