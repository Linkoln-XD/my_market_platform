package com.market.order.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.market.events.catalog.CatalogEventTypes;
import com.market.events.order.OrderEventTypes;
import com.market.events.payment.PaymentEventTypes;
import com.market.events.topics.CatalogTopics;
import com.market.events.topics.OrderTopics;
import com.market.events.topics.PaymentTopics;
import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.catalog.repository.StockRepository;
import com.market.order.order.dto.CancelOrderRequest;
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
class OrderCancelSagaIT {

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

    @Autowired
    private StockRepository stockRepository;

    private KafkaConsumer<String, String> catalogConsumer;
    private KafkaConsumer<String, String> paymentConsumer;
    private final List<ConsumerRecord<String, String>> catalogRecords = new CopyOnWriteArrayList<>();
    private final List<ConsumerRecord<String, String>> paymentRecords = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUpConsumers() {
        catalogConsumer = new KafkaConsumer<>(consumerProps("cancel-saga-catalog"));
        catalogConsumer.subscribe(List.of(CatalogTopics.CATALOG_EVENTS));
        paymentConsumer = new KafkaConsumer<>(consumerProps("cancel-saga-payment"));
        paymentConsumer.subscribe(List.of(PaymentTopics.PAYMENT_EVENTS));
    }

    @AfterEach
    void tearDownConsumers() {
        if (catalogConsumer != null) {
            catalogConsumer.close();
        }
        if (paymentConsumer != null) {
            paymentConsumer.close();
        }
        catalogRecords.clear();
        paymentRecords.clear();
    }

    @Test
    @DisplayName("cancel paid order releases stock and triggers refund stub")
    void cancelPaidOrderRunsCompensations() {
        int stockBefore = stockRepository.findById(CLASSIC_BURGER_ID).orElseThrow().getQuantity();

        UUID orderId = createPaidOrder("cancel-saga-paid", "cancel-saga-key-paid");
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(stockRepository.findById(CLASSIC_BURGER_ID).orElseThrow().getQuantity())
                        .isEqualTo(stockBefore - 1));

        ResponseEntity<JsonNode> cancelled = restTemplate.postForEntity(
                "/api/v1/orders/" + orderId + "/cancel",
                new CancelOrderRequest("Changed mind"),
                JsonNode.class);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody().path("status").asText()).isEqualTo("CANCELLED");

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    drainConsumers();
                    assertThat(stockRepository.findById(CLASSIC_BURGER_ID).orElseThrow().getQuantity())
                            .isEqualTo(stockBefore);
                    assertThat(catalogRecords.stream().map(r -> eventType(r.value())))
                            .anyMatch(CatalogEventTypes.STOCK_RELEASED::equals);
                    assertThat(paymentRecords.stream().map(r -> eventType(r.value())))
                            .anyMatch(PaymentEventTypes.PAYMENT_REFUND_REQUESTED::equals);
                    assertThat(paymentRecords.stream().map(r -> eventType(r.value())))
                            .anyMatch(PaymentEventTypes.PAYMENT_REFUNDED::equals);
                    assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
                });
    }

    @Test
    @DisplayName("cancel pending order publishes OrderCancelled without stock or refund compensation")
    void cancelPendingOrderSkipsCompensations() {
        int stockBefore = stockRepository.findById(CLASSIC_BURGER_ID).orElseThrow().getQuantity();
        UUID orderId = createPendingOrder("cancel-saga-pending", "cancel-saga-key-pending");

        ResponseEntity<JsonNode> cancelled = restTemplate.postForEntity(
                "/api/v1/orders/" + orderId + "/cancel",
                new CancelOrderRequest("No longer needed"),
                JsonNode.class);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    drainConsumers();
                    assertThat(stockRepository.findById(CLASSIC_BURGER_ID).orElseThrow().getQuantity())
                            .isEqualTo(stockBefore);
                    assertThat(catalogRecords).isEmpty();
                    assertThat(paymentRecords.stream().map(r -> eventType(r.value())))
                            .noneMatch(PaymentEventTypes.PAYMENT_REFUND_REQUESTED::equals);
                });
    }

    private UUID createPendingOrder(String owner, String idempotencyKey) {
        restTemplate.postForEntity(
                "/api/v1/carts/" + owner + "/items",
                new AddCartItemRequest(CLASSIC_BURGER_ID, 1),
                JsonNode.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> created = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest(owner), headers),
                JsonNode.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(created.getBody().path("id").asText());
    }

    private UUID createPaidOrder(String owner, String idempotencyKey) {
        UUID orderId = createPendingOrder(owner, idempotencyKey);
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                        .isEqualTo(OrderStatus.PAID));
        return orderId;
    }

    private void drainConsumers() {
        catalogConsumer.poll(Duration.ofMillis(300)).forEach(catalogRecords::add);
        paymentConsumer.poll(Duration.ofMillis(300)).forEach(paymentRecords::add);
    }

    private String eventType(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).path("eventType").asText();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Properties consumerProps(String groupPrefix) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupPrefix + "-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }
}
