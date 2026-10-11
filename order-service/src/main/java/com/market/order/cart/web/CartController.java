package com.market.order.cart.web;

import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.cart.dto.CartResponse;
import com.market.order.cart.dto.UpdateCartItemRequest;
import com.market.order.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts/{ownerId}")
@RequiredArgsConstructor
@Validated
@Tag(name = "Cart", description = "Redis-backed shopping cart keyed by user or session id")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get cart (empty cart if none stored yet)")
    public CartResponse getCart(
            @PathVariable @Size(min = 1, max = 128) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String ownerId) {
        return cartService.getCart(ownerId);
    }

    @PostMapping("/items")
    @Operation(summary = "Add quantity to a cart line (merges with existing line)")
    public CartResponse addItem(
            @PathVariable @Size(min = 1, max = 128) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String ownerId,
            @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(ownerId, request);
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Set absolute quantity for a cart line")
    public CartResponse updateItem(
            @PathVariable @Size(min = 1, max = 128) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String ownerId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(ownerId, productId, request);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove one product from the cart")
    public CartResponse removeItem(
            @PathVariable @Size(min = 1, max = 128) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String ownerId,
            @PathVariable UUID productId) {
        return cartService.removeItem(ownerId, productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear the entire cart")
    public void clearCart(
            @PathVariable @Size(min = 1, max = 128) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String ownerId) {
        cartService.clearCart(ownerId);
    }
}
