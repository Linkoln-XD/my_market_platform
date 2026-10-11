package com.market.order.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "market.security")
public record MarketSecurityProperties(
        Jwt jwt,
        DemoToken demoToken,
        RateLimit rateLimit
) {

    public record Jwt(
            String issuer,
            String secret
    ) {
    }

    public record DemoToken(
            boolean enabled,
            Duration accessTokenTtl
    ) {
    }

    public record RateLimit(
            int createOrderLimit,
            Duration createOrderWindow
    ) {
    }
}
