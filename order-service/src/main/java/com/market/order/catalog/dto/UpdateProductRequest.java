package com.market.order.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 256) String name,
        @Size(max = 4000) String description,
        @Min(0) long priceCents,
        @NotBlank @Size(min = 3, max = 3) String currency,
        boolean active
) {
}
