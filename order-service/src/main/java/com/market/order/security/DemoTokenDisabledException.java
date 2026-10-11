package com.market.order.security;

public class DemoTokenDisabledException extends RuntimeException {

    public DemoTokenDisabledException() {
        super("Demo token issuer is disabled");
    }
}
