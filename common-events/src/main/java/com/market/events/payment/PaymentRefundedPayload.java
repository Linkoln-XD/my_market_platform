package com.market.events.payment;

import java.util.UUID;

public record PaymentRefundedPayload(
        UUID refundId,
        UUID orderId,
        long amountCents,
        String currency
) {
}
