package com.market.order.catalog.dto;

import jakarta.validation.constraints.Min;

public record UpdateStockRequest(@Min(0) int quantity) {
}
