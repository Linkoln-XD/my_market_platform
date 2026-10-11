package com.market.order.cart.service;

import com.market.order.cart.model.CartDocument;
import com.market.order.cart.model.CartItemDocument;
import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.cart.dto.CartItemResponse;
import com.market.order.cart.dto.CartResponse;
import com.market.order.cart.dto.UpdateCartItemRequest;
import com.market.order.cart.store.CartRedisStore;
import com.market.order.catalog.domain.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRedisStore cartRedisStore;
    private final CatalogAvailabilityService catalogAvailabilityService;

    public CartResponse getCart(String ownerId) {
        CartDocument cart = cartRedisStore.find(ownerId)
                .orElseGet(() -> emptyCart(ownerId));
        return toResponse(cart);
    }

    public CartResponse addItem(String ownerId, AddCartItemRequest request) {
        CartDocument cart = cartRedisStore.find(ownerId).orElseGet(() -> emptyCart(ownerId));
        int existing = findItem(cart, request.productId()).map(CartItemDocument::getQuantity).orElse(0);
        int targetQuantity = existing + request.quantity();
        ProductEntity product = catalogAvailabilityService.requireAvailableProduct(
                request.productId(), targetQuantity);
        upsertLine(cart, product, targetQuantity);
        cartRedisStore.save(cart);
        return toResponse(cart);
    }

    public CartResponse updateItem(String ownerId, UUID productId, UpdateCartItemRequest request) {
        CartDocument cart = cartRedisStore.find(ownerId).orElseGet(() -> emptyCart(ownerId));
        ProductEntity product = catalogAvailabilityService.requireAvailableProduct(
                productId, request.quantity());
        upsertLine(cart, product, request.quantity());
        cartRedisStore.save(cart);
        return toResponse(cart);
    }

    public CartResponse removeItem(String ownerId, UUID productId) {
        CartDocument cart = cartRedisStore.find(ownerId).orElseGet(() -> emptyCart(ownerId));
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (cart.getItems().isEmpty()) {
            cartRedisStore.delete(ownerId);
            return toResponse(emptyCart(ownerId));
        }
        cartRedisStore.save(cart);
        return toResponse(cart);
    }

    public void clearCart(String ownerId) {
        cartRedisStore.delete(ownerId);
    }

    private static CartDocument emptyCart(String ownerId) {
        return CartDocument.builder()
                .ownerId(ownerId)
                .items(new ArrayList<>())
                .build();
    }

    private static Optional<CartItemDocument> findItem(CartDocument cart, UUID productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    private static void upsertLine(CartDocument cart, ProductEntity product, int quantity) {
        Iterator<CartItemDocument> iterator = cart.getItems().iterator();
        while (iterator.hasNext()) {
            CartItemDocument item = iterator.next();
            if (item.getProductId().equals(product.getId())) {
                iterator.remove();
                break;
            }
        }
        cart.getItems().add(CartItemDocument.builder()
                .productId(product.getId())
                .sku(product.getSku())
                .productName(product.getName())
                .unitPriceCents(product.getPriceCents())
                .currency(product.getCurrency())
                .quantity(quantity)
                .build());
    }

    private static CartResponse toResponse(CartDocument cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> new CartItemResponse(
                        item.getProductId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getUnitPriceCents(),
                        item.getCurrency(),
                        item.getQuantity(),
                        item.getUnitPriceCents() * item.getQuantity()))
                .toList();
        int totalQuantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        long subtotal = items.stream().mapToLong(CartItemResponse::lineTotalCents).sum();
        String currency = items.isEmpty() ? "USD" : items.getFirst().currency();
        return new CartResponse(cart.getOwnerId(), items, totalQuantity, subtotal, currency, cart.getUpdatedAt());
    }
}
