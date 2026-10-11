package com.market.order.cart;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "market.cart")
public record CartProperties(
        String redisKeyPrefix,
        Duration ttl
) {
}
