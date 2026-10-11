package com.market.order.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 128) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") String slug
) {
}
