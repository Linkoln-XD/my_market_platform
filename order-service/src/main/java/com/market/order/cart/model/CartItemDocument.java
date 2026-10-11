package com.market.order.cart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDocument {

    private UUID productId;
    private String sku;
    private String productName;
    private long unitPriceCents;
    private String currency;
    private int quantity;
}
