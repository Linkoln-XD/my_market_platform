package com.market.order.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderBusinessMetrics {

    private final Counter ordersCreated;

    public OrderBusinessMetrics(MeterRegistry meterRegistry) {
        this.ordersCreated = Counter.builder("market.orders.created")
                .description("Orders committed from checkout API")
                .register(meterRegistry);
    }

    public void recordOrderCreated() {
        ordersCreated.increment();
    }
}
