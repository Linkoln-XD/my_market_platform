package com.market.order.order.dto;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @Size(max = 512) String reason
) {
}
