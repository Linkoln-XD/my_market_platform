package com.market.order.catalog.repository;

import com.market.order.catalog.domain.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockRepository extends JpaRepository<StockEntity, UUID> {
}
