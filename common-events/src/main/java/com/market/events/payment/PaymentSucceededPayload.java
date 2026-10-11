package com.market.events.payment;

import java.util.UUID;

public record PaymentSucceededPayload(
        UUID paymentId,
        UUID orderId,
        long amountCents,
        String currency
) {
}
