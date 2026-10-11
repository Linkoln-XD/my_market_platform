package com.market.order.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market.outbox")
public record OutboxProperties(
        /** Poll delay for {@link OutboxPublisher} (Spring {@code @Scheduled} fixed delay). */
        long pollDelayMs,
        int batchSize
) {
}
