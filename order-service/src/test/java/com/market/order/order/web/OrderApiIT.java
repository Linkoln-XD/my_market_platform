package com.market.order.order.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.order.dto.CreateOrderRequest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OrderApiIT {

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

    @Test
    @DisplayName("POST /orders creates PENDING_PAYMENT order from cart and clears Redis cart")
    void createOrderFromCart() {
        String owner = "checkout-user-1";
        seedCart(owner, 2);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", "order-create-key-001");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> created = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest(owner), headers),
                JsonNode.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().path("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(created.getBody().path("subtotalCents").asLong()).isEqualTo(1798);
        assertThat(created.getBody().path("items")).hasSize(1);

        ResponseEntity<JsonNode> replay = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest(owner), headers),
                JsonNode.class);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(replay.getBody().path("id").asText())
                .isEqualTo(created.getBody().path("id").asText());

        ResponseEntity<JsonNode> cart = restTemplate.getForEntity("/api/v1/carts/" + owner, JsonNode.class);
        assertThat(cart.getBody().path("items")).isEmpty();
    }

    @Test
    @DisplayName("concurrent POST /orders with the same Idempotency-Key yields a single order id")
    void concurrentIdempotentCreateReturnsSingleOrder() throws Exception {
        String owner = "checkout-concurrent";
        seedCart(owner, 1);
        String idempotencyKey = "concurrent-idem-key-001";

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                tasks.add(() -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Idempotency-Key", idempotencyKey);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    ResponseEntity<JsonNode> response = restTemplate.exchange(
                            "/api/v1/orders",
                            HttpMethod.POST,
                            new HttpEntity<>(new CreateOrderRequest(owner), headers),
                            JsonNode.class);
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    return response.getBody().path("id").asText();
                });
            }
            List<Future<String>> futures = pool.invokeAll(tasks);
            List<String> orderIds = futures.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).toList();
            assertThat(orderIds).hasSize(4);
            assertThat(orderIds).allMatch(id -> id.equals(orderIds.getFirst()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("empty cart returns 400 on checkout")
    void emptyCartRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", "empty-cart-key-001");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest("empty-cart-owner"), headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("title").asText()).isEqualTo("Cart is empty");
    }

    private void seedCart(String owner, int quantity) {
        restTemplate.postForEntity(
                "/api/v1/carts/" + owner + "/items",
                new AddCartItemRequest(CLASSIC_BURGER_ID, quantity),
                JsonNode.class);
    }
}
