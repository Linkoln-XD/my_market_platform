package com.market.order.cart.service;

import com.market.order.cart.InsufficientStockException;
import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.cart.model.CartDocument;
import com.market.order.cart.store.CartRedisStore;
import com.market.order.catalog.domain.CategoryEntity;
import com.market.order.catalog.domain.ProductEntity;
import com.market.order.catalog.domain.StockEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String OWNER = "user-demo-1";
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");

    @Mock
    private CartRedisStore cartRedisStore;
    @Mock
    private CatalogAvailabilityService catalogAvailabilityService;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("addItem merges quantity with an existing line")
    void addItemMergesQuantity() {
        CartDocument existing = CartDocument.builder()
                .ownerId(OWNER)
                .items(new ArrayList<>())
                .build();
        existing.getItems().add(com.market.order.cart.model.CartItemDocument.builder()
                .productId(PRODUCT_ID)
                .sku("BRG-CLASSIC")
                .productName("Classic Burger")
                .unitPriceCents(899)
                .currency("USD")
                .quantity(2)
                .build());

        when(cartRedisStore.find(OWNER)).thenReturn(Optional.of(existing));
        when(catalogAvailabilityService.requireAvailableProduct(eq(PRODUCT_ID), eq(5)))
                .thenReturn(sampleProduct(120));

        var response = cartService.addItem(OWNER, new AddCartItemRequest(PRODUCT_ID, 3));

        assertThat(response.totalQuantity()).isEqualTo(5);
        assertThat(response.items()).singleElement()
                .satisfies(item -> assertThat(item.quantity()).isEqualTo(5));
        verify(cartRedisStore).save(existing);
    }

    @Test
    @DisplayName("addItem propagates insufficient stock from catalog validation")
    void addItemPropagatesInsufficientStock() {
        when(cartRedisStore.find(OWNER)).thenReturn(Optional.empty());
        when(catalogAvailabilityService.requireAvailableProduct(any(), eq(11)))
                .thenThrow(new InsufficientStockException("no stock"));

        assertThatThrownBy(() -> cartService.addItem(OWNER, new AddCartItemRequest(PRODUCT_ID, 11)))
                .isInstanceOf(InsufficientStockException.class);
    }

    private static ProductEntity sampleProduct(int stockQty) {
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.randomUUID());
        category.setName("Burgers");

        ProductEntity product = new ProductEntity();
        product.setId(PRODUCT_ID);
        product.setCategory(category);
        product.setSku("BRG-CLASSIC");
        product.setName("Classic Burger");
        product.setPriceCents(899);
        product.setCurrency("USD");
        product.setActive(true);

        StockEntity stock = new StockEntity();
        stock.setProduct(product);
        stock.setProductId(PRODUCT_ID);
        stock.setQuantity(stockQty);
        product.setStock(stock);
        return product;
    }
}
