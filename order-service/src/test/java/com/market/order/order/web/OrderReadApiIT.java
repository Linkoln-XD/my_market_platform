package com.market.order.order.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.order.dto.CreateOrderRequest;
import com.market.order.security.MarketSecurityProperties;
import com.market.order.support.AuthTestSupport;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OrderReadApiIT {

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
    private JwtEncoder jwtEncoder;

    @Autowired
    private MarketSecurityProperties securityProperties;

    @Test
    @DisplayName("GET /orders/{id} returns order for owner")
    void ownerCanReadOrder() {
        String owner = "read-api-owner";
        UUID orderId = createOrder(owner, "read-api-key-001");

        HttpHeaders headers = AuthTestSupport.bearerHeaders(jwtEncoder, securityProperties, owner);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("id").asText()).isEqualTo(orderId.toString());
        assertThat(response.getBody().path("ownerId").asText()).isEqualTo(owner);
    }

    @Test
    @DisplayName("GET /orders/{id} forbidden for another user")
    void otherUserCannotReadOrder() {
        String owner = "read-api-owner-2";
        UUID orderId = createOrder(owner, "read-api-key-002");

        HttpHeaders headers = AuthTestSupport.bearerHeaders(jwtEncoder, securityProperties, "intruder");
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /admin/orders requires ADMIN")
    void adminCanListOrders() {
        createOrder("admin-list-owner", "read-api-key-003");

        HttpHeaders userHeaders = AuthTestSupport.bearerHeaders(jwtEncoder, securityProperties, "regular-user");
        ResponseEntity<JsonNode> forbidden = restTemplate.exchange(
                "/api/v1/admin/orders",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                JsonNode.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        HttpHeaders adminHeaders = AuthTestSupport.bearerHeaders(
                jwtEncoder, securityProperties, "catalog-admin", java.util.List.of("ADMIN"));
        ResponseEntity<JsonNode> ok = restTemplate.exchange(
                "/api/v1/admin/orders",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                JsonNode.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody().path("content").isArray()).isTrue();
        assertThat(ok.getBody().path("totalElements").asInt()).isPositive();
    }

    private UUID createOrder(String owner, String idempotencyKey) {
        restTemplate.postForEntity(
                "/api/v1/carts/" + owner + "/items",
                new AddCartItemRequest(CLASSIC_BURGER_ID, 1),
                JsonNode.class);

        HttpHeaders headers = AuthTestSupport.bearerHeaders(jwtEncoder, securityProperties, owner);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);

        ResponseEntity<JsonNode> created = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest(owner), headers),
                JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(created.getBody().path("id").asText());
    }
}
