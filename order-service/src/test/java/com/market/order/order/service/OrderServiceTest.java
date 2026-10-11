package com.market.order.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.market.order.cart.model.CartDocument;
import com.market.order.cart.model.CartItemDocument;
import com.market.order.cart.service.CartService;
import com.market.order.cart.service.CatalogAvailabilityService;
import com.market.order.order.OrderProperties;
import com.market.order.order.OrderStatus;
import com.market.order.order.domain.IdempotencyRecordEntity;
import com.market.order.order.dto.CreateOrderRequest;
import com.market.order.order.dto.OrderResponse;
import com.market.order.order.mapper.OrderMapper;
import com.market.order.order.repository.IdempotencyRecordRepository;
import com.market.order.order.repository.OrderRepository;
import com.market.order.observability.OrderBusinessMetrics;
import com.market.order.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String OWNER = "user-order-1";
    private static final String IDEMPOTENCY_KEY = "idem-key-001";

    @Mock
    private CartService cartService;
    @Mock
    private CatalogAvailabilityService catalogAvailabilityService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private TransactionAdvisoryLock transactionAdvisoryLock;
    @Mock
    private OutboxService outboxService;
    @Mock
    private OrderBusinessMetrics orderBusinessMetrics;

    private ObjectMapper objectMapper;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        orderService = new OrderService(
                cartService,
                catalogAvailabilityService,
                orderRepository,
                idempotencyRecordRepository,
                orderMapper,
                objectMapper,
                new OrderProperties(Duration.ofHours(24)),
                transactionAdvisoryLock,
                outboxService,
                orderBusinessMetrics);
    }

    @Test
    @DisplayName("createOrder replays stored response when idempotency key exists")
    void createOrderReplaysIdempotentResponse() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse stored = new OrderResponse(
                orderId, OWNER, OrderStatus.PENDING_PAYMENT, "USD", 899,
                java.util.List.of(), Instant.parse("2026-01-01T00:00:00Z"));
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setResponseJson(objectMapper.writeValueAsString(stored));

        when(idempotencyRecordRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(record));

        OrderResponse response = orderService.createOrder(new CreateOrderRequest(OWNER), IDEMPOTENCY_KEY);

        assertThat(response.id()).isEqualTo(orderId);
        verify(cartService, never()).requireNonEmptyCart(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder persists PENDING_PAYMENT and clears cart")
    void createOrderPersistsPendingPayment() {
        UUID productId = UUID.randomUUID();
        CartDocument cart = CartDocument.builder()
                .ownerId(OWNER)
                .items(new ArrayList<>())
                .build();
        cart.getItems().add(CartItemDocument.builder()
                .productId(productId)
                .sku("SKU-1")
                .productName("Item")
                .unitPriceCents(500)
                .currency("USD")
                .quantity(2)
                .build());

        when(idempotencyRecordRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(cartService.requireNonEmptyCart(OWNER)).thenReturn(cart);
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any())).thenAnswer(invocation -> {
            var order = invocation.getArgument(0, com.market.order.order.domain.OrderEntity.class);
            return new OrderResponse(
                    order.getId(), order.getOwnerId(), order.getStatus(), order.getCurrency(),
                    order.getSubtotalCents(), java.util.List.of(), order.getCreatedAt());
        });

        OrderResponse response = orderService.createOrder(new CreateOrderRequest(OWNER), IDEMPOTENCY_KEY);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        ArgumentCaptor<com.market.order.order.domain.OrderEntity> orderCaptor =
                ArgumentCaptor.forClass(com.market.order.order.domain.OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(orderCaptor.getValue().getSubtotalCents()).isEqualTo(1000);
        verify(catalogAvailabilityService).requireAvailableProduct(productId, 2);
        verify(outboxService).enqueueOrderCreated(orderCaptor.getValue());
        verify(cartService).clearCart(OWNER);
        verify(idempotencyRecordRepository).save(any(IdempotencyRecordEntity.class));
    }
}
