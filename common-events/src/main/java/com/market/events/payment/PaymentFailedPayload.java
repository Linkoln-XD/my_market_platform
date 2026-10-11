package com.market.events.payment;

import java.util.UUID;

public record PaymentFailedPayload(
        UUID paymentId,
        UUID orderId,
        long amountCents,
        String currency,
        String reason
) {
}
