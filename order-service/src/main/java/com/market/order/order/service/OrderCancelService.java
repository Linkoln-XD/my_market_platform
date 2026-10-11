package com.market.order.order.service;

import com.market.order.order.InvalidOrderStateException;
import com.market.order.order.OrderOwnerMismatchException;
import com.market.order.order.OrderNotFoundException;
import com.market.order.order.OrderStatus;
import com.market.order.order.domain.OrderEntity;
import com.market.order.order.dto.CancelOrderRequest;
import com.market.order.order.dto.OrderResponse;
import com.market.order.order.mapper.OrderMapper;
import com.market.order.order.repository.OrderRepository;
import com.market.order.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCancelService {

    private static final EnumSet<OrderStatus> CANCELLABLE = EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request, String authenticatedSubject) {
        OrderEntity order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getOwnerId().equals(authenticatedSubject)) {
            throw new OrderOwnerMismatchException("Only the order owner can cancel this order");
        }
        if (!CANCELLABLE.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "Order " + orderId + " cannot be cancelled from status " + order.getStatus());
        }

        String previousStatus = order.getStatus().name();
        String reason = request.reason() == null || request.reason().isBlank()
                ? "Cancelled by customer"
                : request.reason();

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        outboxService.enqueueOrderCancelled(order, previousStatus, reason);
        return orderMapper.toResponse(order);
    }
}
