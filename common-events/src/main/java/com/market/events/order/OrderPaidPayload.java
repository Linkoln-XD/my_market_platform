package com.market.events.order;

import java.util.UUID;

public record OrderPaidPayload(
        UUID orderId,
        UUID paymentId,
        long amountCents,
        String currency,
        String status
) {
}
