package com.market.order.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties(prefix = "market.payment.mock")
public record PaymentMockProperties(
        /** When set, mock gateway declines payment for this order id (tests). */
        UUID forceDeclineOrderId
) {
}
