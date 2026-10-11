package com.market.notification.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market.security.jwt")
public record MarketJwtProperties(
        String issuer,
        String secret
) {
}
