package com.market.order.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.order.cart.model.CartDocument;
import com.market.order.cart.model.CartItemDocument;
import com.market.order.cart.service.CatalogAvailabilityService;
import com.market.order.cart.service.CartService;
import com.market.order.order.EmptyCartException;
import com.market.order.order.OrderProperties;
import com.market.order.order.OrderStatus;
import com.market.order.order.domain.IdempotencyRecordEntity;
import com.market.order.order.domain.OrderEntity;
import com.market.order.order.domain.OrderItemEntity;
import com.market.order.order.dto.CreateOrderRequest;
import com.market.order.order.dto.OrderResponse;
import com.market.order.order.mapper.OrderMapper;
import com.market.order.order.repository.IdempotencyRecordRepository;
import com.market.order.order.repository.OrderRepository;
import com.market.order.observability.OrderBusinessMetrics;
import com.market.order.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Creates orders from the Redis cart. Stock is validated against PostgreSQL but not decremented here;
 * reservation runs through {@code catalog.events} consumers (step 4+). Payment is Kafka-only after
 * {@code OrderCreated} is published from the outbox (step 4).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final CatalogAvailabilityService catalogAvailabilityService;
    private final OrderRepository orderRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final OrderProperties orderProperties;
    private final TransactionAdvisoryLock transactionAdvisoryLock;
    private final OutboxService outboxService;
    private final OrderBusinessMetrics orderBusinessMetrics;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        transactionAdvisoryLock.lock("order-idempotency", idempotencyKey);
        return idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
                .map(record -> readStoredResponse(record.getResponseJson()))
                .orElseGet(() -> createOrderAndRecord(request, idempotencyKey));
    }

    private OrderResponse createOrderAndRecord(CreateOrderRequest request, String idempotencyKey) {
        CartDocument cart = cartService.requireNonEmptyCart(request.ownerId());
        validateCartLinesAgainstCatalog(cart);

        OrderEntity order = new OrderEntity();
        order.setOwnerId(request.ownerId());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCurrency(cartCurrency(cart));
        order.setSubtotalCents(cartSubtotal(cart));

        for (CartItemDocument line : cart.getItems()) {
            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setProductId(line.getProductId());
            item.setSku(line.getSku());
            item.setProductName(line.getProductName());
            item.setQuantity(line.getQuantity());
            item.setUnitPriceCents(line.getUnitPriceCents());
            item.setLineTotalCents(line.getUnitPriceCents() * line.getQuantity());
            order.getItems().add(item);
        }

        OrderEntity saved = orderRepository.save(order);
        outboxService.enqueueOrderCreated(saved);
        OrderResponse response = orderMapper.toResponse(saved);

        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setIdempotencyKey(idempotencyKey);
        record.setOwnerId(request.ownerId());
        record.setOrderId(saved.getId());
        record.setResponseJson(writeResponse(response));
        record.setCreatedAt(Instant.now());
        record.setExpiresAt(Instant.now().plus(orderProperties.idempotencyTtl()));

        try {
            idempotencyRecordRepository.save(record);
        } catch (DataIntegrityViolationException duplicateKey) {
            return idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existing -> readStoredResponse(existing.getResponseJson()))
                    .orElseThrow(() -> duplicateKey);
        }

        cartService.clearCart(request.ownerId());
        orderBusinessMetrics.recordOrderCreated();
        return response;
    }

    private void validateCartLinesAgainstCatalog(CartDocument cart) {
        for (CartItemDocument line : cart.getItems()) {
            catalogAvailabilityService.requireAvailableProduct(line.getProductId(), line.getQuantity());
        }
    }

    private static String cartCurrency(CartDocument cart) {
        return cart.getItems().getFirst().getCurrency();
    }

    private static long cartSubtotal(CartDocument cart) {
        return cart.getItems().stream()
                .mapToLong(line -> line.getUnitPriceCents() * line.getQuantity())
                .sum();
    }

    private OrderResponse readStoredResponse(String json) {
        try {
            return objectMapper.readValue(json, OrderResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored idempotency payload is invalid", ex);
        }
    }

    private String writeResponse(OrderResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize order response", ex);
        }
    }
}
