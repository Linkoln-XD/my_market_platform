package com.market.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Single deployable for the order flow until notification-service is split out in step 7.
 *
 * <p>Catalog rows live in this process (schema {@code catalog}). Stock reservation still
 * crosses Kafka on {@code catalog.events}: a listener reserves or releases stock.
 * That boundary stays a Kafka consumer, including while both sides share this JVM.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableKafka
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
