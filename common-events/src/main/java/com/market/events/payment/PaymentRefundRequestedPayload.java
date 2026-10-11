package com.market.events.payment;

import java.util.UUID;

public record PaymentRefundRequestedPayload(
        UUID refundId,
        UUID orderId,
        long amountCents,
        String currency,
        String reason
) {
}
