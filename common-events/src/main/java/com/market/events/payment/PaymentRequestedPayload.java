package com.market.events.payment;

import java.util.UUID;

public record PaymentRequestedPayload(
        UUID paymentId,
        UUID orderId,
        long amountCents,
        String currency
) {
}
