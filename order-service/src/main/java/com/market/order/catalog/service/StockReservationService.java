package com.market.order.catalog.service;

import com.market.order.cart.InsufficientStockException;
import com.market.order.catalog.domain.StockEntity;
import com.market.order.catalog.repository.StockRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockRepository stockRepository;

    @Transactional
    public int reserve(UUID productId, int quantity) {
        StockEntity stock = stockRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Stock row missing for product " + productId));
        if (stock.getQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Cannot reserve " + quantity + " for product " + productId + ", on hand " + stock.getQuantity());
        }
        stock.setQuantity(stock.getQuantity() - quantity);
        try {
            return stockRepository.saveAndFlush(stock).getQuantity();
        } catch (OptimisticLockException ex) {
            throw new IllegalStateException("Concurrent stock update for product " + productId, ex);
        }
    }
}
