package com.market.order.order.repository;

import com.market.order.order.domain.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsById(UUID id);
}
