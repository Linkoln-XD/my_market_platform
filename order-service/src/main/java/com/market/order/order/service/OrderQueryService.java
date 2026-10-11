package com.market.order.order.service;

import com.market.order.order.OrderNotFoundException;
import com.market.order.order.OrderOwnerMismatchException;
import com.market.order.order.OrderStatus;
import com.market.order.order.domain.OrderEntity;
import com.market.order.order.dto.OrderPageResponse;
import com.market.order.order.dto.OrderResponse;
import com.market.order.order.mapper.OrderMapper;
import com.market.order.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, String subject, boolean admin) {
        OrderEntity order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        assertCanAccess(order, subject, admin);
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse listForOwner(String ownerId, Pageable pageable) {
        Page<OrderEntity> page = orderRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public OrderPageResponse listAll(OrderStatus status, Pageable pageable) {
        Page<OrderEntity> page = status == null
                ? orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                : orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return toPageResponse(page);
    }

    private static void assertCanAccess(OrderEntity order, String subject, boolean admin) {
        if (!admin && !order.getOwnerId().equals(subject)) {
            throw new OrderOwnerMismatchException("Only the order owner can view this order");
        }
    }

    private OrderPageResponse toPageResponse(Page<OrderEntity> page) {
        return new OrderPageResponse(
                page.getContent().stream().map(orderMapper::toResponse).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }
}
