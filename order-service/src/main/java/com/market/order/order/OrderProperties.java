package com.market.order.order;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "market.order")
public record OrderProperties(Duration idempotencyTtl) {
}
