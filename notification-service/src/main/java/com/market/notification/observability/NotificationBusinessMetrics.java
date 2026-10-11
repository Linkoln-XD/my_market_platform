package com.market.notification.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationBusinessMetrics {

    private final MeterRegistry meterRegistry;

    public NotificationBusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordDelivered(String eventType) {
        meterRegistry.counter("market.notifications.delivered", "event_type", eventType).increment();
    }
}
