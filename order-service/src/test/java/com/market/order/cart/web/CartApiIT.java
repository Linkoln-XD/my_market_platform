package com.market.order.cart.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.cart.dto.UpdateCartItemRequest;
import com.market.order.cart.store.CartRedisStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class CartApiIT {

    private static final String OWNER = "session-test-42";
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
    private CartRedisStore cartRedisStore;

    @Test
    @DisplayName("cart CRUD persists in Redis with TTL and validates stock from catalog")
    void cartCrudAndStockValidation() {
        String base = "/api/v1/carts/" + OWNER;

        ResponseEntity<JsonNode> empty = restTemplate.getForEntity(base, JsonNode.class);
        assertThat(empty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(empty.getBody().path("items")).isEmpty();

        ResponseEntity<JsonNode> added = restTemplate.postForEntity(
                base + "/items",
                new AddCartItemRequest(CLASSIC_BURGER_ID, 2),
                JsonNode.class);
        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(added.getBody().path("totalQuantity").asInt()).isEqualTo(2);
        assertThat(added.getBody().path("subtotalCents").asLong()).isEqualTo(1798);

        Duration ttl = cartRedisStore.ttl(OWNER);
        assertThat(ttl).isGreaterThan(Duration.ofHours(1));

        ResponseEntity<JsonNode> merged = restTemplate.postForEntity(
                base + "/items",
                new AddCartItemRequest(CLASSIC_BURGER_ID, 3),
                JsonNode.class);
        assertThat(merged.getBody().path("totalQuantity").asInt()).isEqualTo(5);

        ResponseEntity<ProblemDetail> tooMuch = restTemplate.postForEntity(
                base + "/items",
                new AddCartItemRequest(CLASSIC_BURGER_ID, 200),
                ProblemDetail.class);
        assertThat(tooMuch.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(tooMuch.getBody()).isNotNull();
        assertThat(tooMuch.getBody().getTitle()).isEqualTo("Insufficient stock");

        ResponseEntity<JsonNode> updated = restTemplate.exchange(
                base + "/items/" + CLASSIC_BURGER_ID,
                HttpMethod.PUT,
                new HttpEntity<>(new UpdateCartItemRequest(4)),
                JsonNode.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("items").get(0).path("quantity").asInt()).isEqualTo(4);

        ResponseEntity<JsonNode> removed = restTemplate.exchange(
                base + "/items/" + CLASSIC_BURGER_ID,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                JsonNode.class);
        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(removed.getBody().path("items")).isEmpty();
        assertThat(cartRedisStore.ttl(OWNER)).isEqualTo(Duration.ZERO);
    }
}
