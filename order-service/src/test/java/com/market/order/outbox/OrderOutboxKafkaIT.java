package com.market.order.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.events.order.OrderEventTypes;
import com.market.events.topics.OrderTopics;
import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.order.dto.CreateOrderRequest;
import com.market.order.security.MarketSecurityProperties;
import com.market.order.support.AuthTestSupport;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OrderOutboxKafkaIT {

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
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private MarketSecurityProperties securityProperties;

    private KafkaConsumer<String, String> consumer;
    private final List<ConsumerRecord<String, String>> received = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUpConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-outbox-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(OrderTopics.ORDER_EVENTS));
    }

    @AfterEach
    void tearDownConsumer() {
        if (consumer != null) {
            consumer.close();
        }
        received.clear();
    }

    @Test
    @DisplayName("order commit writes outbox row and publisher emits OrderCreated to order.events")
    void orderCreatedIsPublishedThroughOutbox() throws Exception {
        String owner = "outbox-user-1";
        restTemplate.postForEntity(
                "/api/v1/carts/" + owner + "/items",
                new AddCartItemRequest(CLASSIC_BURGER_ID, 1),
                JsonNode.class);

        HttpHeaders headers = AuthTestSupport.bearerHeaders(jwtEncoder, securityProperties, owner);
        headers.set("Idempotency-Key", "outbox-kafka-key-001");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> created = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest(owner), headers),
                JsonNode.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID orderId = UUID.fromString(created.getBody().path("id").asText());

        OutboxRow outboxRow = findOutboxRow(orderId);
        assertThat(outboxRow.eventType()).isEqualTo(OrderEventTypes.ORDER_CREATED);
        assertThat(outboxRow.topic()).isEqualTo(OrderTopics.ORDER_EVENTS);
        assertThat(outboxRow.publishedAt()).isNull();

        JsonNode payload = objectMapper.readTree(outboxRow.payloadJson());
        UUID eventId = UUID.fromString(payload.path("eventId").asText());
        assertThat(payload.path("eventType").asText()).isEqualTo(OrderEventTypes.ORDER_CREATED);
        assertThat(payload.path("occurredAt").asText()).isNotBlank();
        assertThat(payload.path("payload").path("orderId").asText()).isEqualTo(orderId.toString());

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    drainConsumer();
                    assertThat(received).anySatisfy(record -> {
                        assertThat(record.topic()).isEqualTo(OrderTopics.ORDER_EVENTS);
                        assertThat(record.key()).isEqualTo(orderId.toString());
                    });
                });

        ConsumerRecord<String, String> kafkaRecord = received.stream()
                .filter(r -> orderId.toString().equals(r.key()))
                .findFirst()
                .orElseThrow();
        JsonNode kafkaPayload = objectMapper.readTree(kafkaRecord.value());
        assertThat(kafkaPayload.path("eventId").asText()).isEqualTo(eventId.toString());

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> assertThat(findOutboxRow(orderId).publishedAt()).isNotNull());
    }

    private void drainConsumer() {
        consumer.poll(Duration.ofMillis(200)).forEach(received::add);
    }

    private OutboxRow findOutboxRow(UUID orderId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select event_type, topic, payload::text, published_at
                     from ordering.outbox
                     where aggregate_id = ?
                     """)) {
            statement.setObject(1, orderId);
            try (ResultSet rs = statement.executeQuery()) {
                assertThat(rs.next()).isTrue();
                return new OutboxRow(
                        rs.getString("event_type"),
                        rs.getString("topic"),
                        rs.getString("payload"),
                        rs.getTimestamp("published_at"));
            }
        }
    }

    private record OutboxRow(String eventType, String topic, String payloadJson, java.sql.Timestamp publishedAt) {
    }
}
