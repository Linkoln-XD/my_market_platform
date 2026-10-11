package com.market.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market.notification")
public record NotificationProperties(
        String dlqTopic,
        String consumerName
) {
}
