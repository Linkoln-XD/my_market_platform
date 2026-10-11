package com.market.order.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotBlank
        @Size(min = 1, max = 128)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
        String ownerId
) {
}
