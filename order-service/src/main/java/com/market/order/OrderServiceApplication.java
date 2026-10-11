package com.market.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single deployable for the order flow until notification-service is split out in step 7.
 *
 * <p>Catalog rows live in this process (schema {@code catalog}). Stock reservation still
 * crosses Kafka on {@code catalog.events}: a listener reserves or releases stock.
 * That boundary stays a Kafka consumer, including while both sides share this JVM.
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
