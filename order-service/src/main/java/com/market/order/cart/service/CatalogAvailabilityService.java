package com.market.order.cart.service;

import com.market.order.catalog.CatalogNotFoundException;
import com.market.order.catalog.domain.ProductEntity;
import com.market.order.catalog.domain.StockEntity;
import com.market.order.catalog.repository.ProductRepository;
import com.market.order.cart.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reads catalog stock from PostgreSQL (not Redis cache) so cart limits match current inventory.
 */
@Service
@RequiredArgsConstructor
public class CatalogAvailabilityService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductEntity requireAvailableProduct(UUID productId, int requestedQuantity) {
        ProductEntity product = productRepository.findWithDetailsById(productId)
                .orElseThrow(() -> new CatalogNotFoundException("Product not found: " + productId));
        if (!product.isActive()) {
            throw new CatalogNotFoundException("Product is not available: " + productId);
        }
        StockEntity stock = product.getStock();
        int available = stock != null ? stock.getQuantity() : 0;
        if (requestedQuantity > available) {
            throw new InsufficientStockException(
                    "Requested quantity " + requestedQuantity + " exceeds available stock " + available
                            + " for product " + productId);
        }
        return product;
    }
}
