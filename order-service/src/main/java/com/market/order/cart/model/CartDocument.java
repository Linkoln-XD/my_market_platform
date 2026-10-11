package com.market.order.cart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDocument {

    private String ownerId;

    @Builder.Default
    private List<CartItemDocument> items = new ArrayList<>();

    private Instant updatedAt;
}
