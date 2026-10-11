package com.market.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "market.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
