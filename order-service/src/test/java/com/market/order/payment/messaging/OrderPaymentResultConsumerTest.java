package com.market.order.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.market.events.payment.PaymentEventTypes;
import com.market.events.payment.PaymentSucceededEvent;
import com.market.events.payment.PaymentSucceededPayload;
import com.market.order.messaging.ProcessedEventService;
import com.market.order.order.OrderStatus;
import com.market.order.order.domain.OrderEntity;
import com.market.order.order.repository.OrderRepository;
import com.market.order.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentResultConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PAYMENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private ProcessedEventService processedEventService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OrderPaymentResultConsumer consumer;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new OrderPaymentResultConsumer(objectMapper, processedEventService, orderRepository, outboxService);
    }

    @Test
    @DisplayName("duplicate PaymentSucceeded eventId does not update the order twice")
    void duplicatePaymentSucceededIsIgnored() throws Exception {
        when(processedEventService.markProcessedIfAbsent(
                eq(OrderPaymentResultConsumer.CONSUMER_NAME), eq(EVENT_ID))).thenReturn(false);

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                EVENT_ID,
                Instant.now(),
                PaymentEventTypes.PAYMENT_SUCCEEDED,
                new PaymentSucceededPayload(PAYMENT_ID, ORDER_ID, 899, "USD"));

        consumer.onPaymentResult(objectMapper.writeValueAsString(event));

        verify(orderRepository, never()).save(any());
        verify(outboxService, never()).enqueueOrderPaid(any(), any(), any(Long.class));
    }

    @Test
    @DisplayName("PaymentSucceeded moves pending order to PAID and enqueues OrderPaid")
    void paymentSucceededUpdatesOrder() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCurrency("USD");

        when(processedEventService.markProcessedIfAbsent(
                eq(OrderPaymentResultConsumer.CONSUMER_NAME), eq(EVENT_ID))).thenReturn(true);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                EVENT_ID,
                Instant.now(),
                PaymentEventTypes.PAYMENT_SUCCEEDED,
                new PaymentSucceededPayload(PAYMENT_ID, ORDER_ID, 899, "USD"));

        consumer.onPaymentResult(objectMapper.writeValueAsString(event));

        verify(orderRepository).save(order);
        verify(outboxService).enqueueOrderPaid(order, PAYMENT_ID, 899);
    }
}
