package com.market.order.payment.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.market.events.order.OrderEventTypes;
import com.market.events.payment.PaymentEventTypes;
import com.market.events.topics.OrderTopics;
import com.market.events.topics.PaymentTopics;
import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.order.dto.CreateOrderRequest;
import com.market.order.order.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PaymentFlowIT {

    private static final UUID CLASSIC_BURGER_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    @Container
    @ServiceConnection
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.2"));

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    private KafkaConsumer<String, String> paymentConsumer;
    private KafkaConsumer<String, String> orderConsumer;
    private final List<ConsumerRecord<String, String>> paymentRecords = new CopyOnWriteArrayList<>();
    private final List<ConsumerRecord<String, String>> orderRecords = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUpKafkaConsumers() {
        paymentConsumer = new KafkaConsumer<>(consumerProps("payment-flow-payment"));
        paymentConsumer.subscribe(List.of(PaymentTopics.PAYMENT_EVENTS));
        orderConsumer = new KafkaConsumer<>(consumerProps("payment-flow-order"));
        orderConsumer.subscribe(List.of(OrderTopics.ORDER_EVENTS));
    }

    @AfterEach
    void tearDownKafkaConsumers() {
        if (paymentConsumer != null) {
            paymentConsumer.close();
        }
        if (orderConsumer != null) {
            orderConsumer.close();
        }
        paymentRecords.clear();
        orderRecords.clear();
    }

    @Test
    @DisplayName("OrderCreated drives payment.events and returns OrderPaid on order.events")
    void kafkaPaymentFlowUpdatesOrderToPaid() {
        String owner = "payment-flow-user";
        restTemplate.postForEntity(
                "/api/v1/carts/" + owner + "/items",
                new AddCartItemRequest(CLASSIC_BURGER_ID, 1),
                JsonNode.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", "payment-flow-key-001");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> created = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest(owner), headers),
                JsonNode.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID orderId = UUID.fromString(created.getBody().path("id").asText());

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    drainConsumers();
                    assertThat(paymentRecords.stream().map(r -> readType(r.value())))
                            .anyMatch(PaymentEventTypes.PAYMENT_REQUESTED::equals);
                    assertThat(paymentRecords.stream().map(r -> readType(r.value())))
                            .anyMatch(PaymentEventTypes.PAYMENT_SUCCEEDED::equals);
                    assertThat(orderRecords.stream().map(r -> readType(r.value())))
                            .anyMatch(OrderEventTypes.ORDER_PAID::equals);
                    assertThat(orderRepository.findById(orderId)).isPresent();
                    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus().name())
                            .isEqualTo("PAID");
                });
    }

    private void drainConsumers() {
        paymentConsumer.poll(Duration.ofMillis(300)).forEach(paymentRecords::add);
        orderConsumer.poll(Duration.ofMillis(300)).forEach(orderRecords::add);
    }

    private String readType(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).path("eventType").asText();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Properties consumerProps(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }
}
