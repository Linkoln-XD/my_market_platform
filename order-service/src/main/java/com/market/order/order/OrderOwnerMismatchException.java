package com.market.order.order;

public class OrderOwnerMismatchException extends RuntimeException {

    public OrderOwnerMismatchException(String message) {
        super(message);
    }
}
