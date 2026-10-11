package com.market.order.security.dto;

import java.time.Instant;

public record DemoTokenResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt
) {
}
