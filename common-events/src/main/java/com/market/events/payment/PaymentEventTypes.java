package com.market.events.payment;

public final class PaymentEventTypes {

    public static final String PAYMENT_REQUESTED = "PaymentRequested";
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String PAYMENT_REFUND_REQUESTED = "PaymentRefundRequested";
    public static final String PAYMENT_REFUNDED = "PaymentRefunded";

    private PaymentEventTypes() {
    }
}
