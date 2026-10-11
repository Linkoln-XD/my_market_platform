package com.market.order.order.repository;

import com.market.order.order.OrderStatus;
import com.market.order.order.domain.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsById(UUID id);

    @EntityGraph(attributePaths = "items")
    Page<OrderEntity> findByOwnerIdOrderByCreatedAtDesc(String ownerId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Page<OrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Page<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
}
