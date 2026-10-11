package com.market.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.market.events.order.OrderEventTypes;
import com.market.events.order.OrderPaidEvent;
import com.market.events.order.OrderPaidPayload;
import com.market.events.topics.OrderTopics;
import com.market.notification.repository.DeliveredNotificationRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OrderPaidNotificationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    @Container
    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.2"));

    @Autowired
    private DeliveredNotificationRepository deliveredNotificationRepository;

    @Test
    @DisplayName("OrderPaid on order.events creates a delivered notification row")
    void orderPaidEventIsDelivered() throws Exception {
        UUID eventId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddd01");
        UUID orderId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID paymentId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OrderPaidEvent event = new OrderPaidEvent(
                eventId,
                Instant.parse("2026-01-15T12:00:00Z"),
                OrderEventTypes.ORDER_PAID,
                new OrderPaidPayload(orderId, paymentId, 899, "USD", "PAID"));
        String json = mapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps())) {
            producer.send(new ProducerRecord<>(OrderTopics.ORDER_EVENTS, orderId.toString(), json)).get();
        }

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(deliveredNotificationRepository.findByEventId(eventId)).isPresent());

        var delivered = deliveredNotificationRepository.findByEventId(eventId).orElseThrow();
        assertThat(delivered.getOrderId()).isEqualTo(orderId);
        assertThat(delivered.getEventType()).isEqualTo(OrderEventTypes.ORDER_PAID);
        assertThat(delivered.getMessage()).contains("OrderPaid");
    }

    @Test
    @DisplayName("duplicate OrderPaid eventId does not create a second delivery row")
    void duplicateEventIsIdempotent() throws Exception {
        UUID eventId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddd02");
        UUID orderId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02");

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OrderPaidEvent event = new OrderPaidEvent(
                eventId,
                Instant.now(),
                OrderEventTypes.ORDER_PAID,
                new OrderPaidPayload(orderId, UUID.randomUUID(), 500, "USD", "PAID"));
        String json = mapper.writeValueAsString(event);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps())) {
            producer.send(new ProducerRecord<>(OrderTopics.ORDER_EVENTS, orderId.toString(), json)).get();
            producer.send(new ProducerRecord<>(OrderTopics.ORDER_EVENTS, orderId.toString(), json)).get();
        }

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(deliveredNotificationRepository.findByEventId(eventId)).isPresent());

        assertThat(deliveredNotificationRepository.countByEventId(eventId)).isEqualTo(1);
    }

    private static Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }
}
