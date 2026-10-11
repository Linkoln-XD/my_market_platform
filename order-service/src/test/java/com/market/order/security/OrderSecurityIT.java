package com.market.order.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.market.order.cart.dto.AddCartItemRequest;
import com.market.order.order.dto.CreateOrderRequest;
import com.market.order.support.AuthTestSupport;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(properties = {
        "market.security.rate-limit.create-order-limit=2",
        "market.security.rate-limit.create-order-window=1m"
})
@Testcontainers
class OrderSecurityIT {

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
    @DisplayName("POST /orders without JWT returns 401")
    void createOrderRequiresAuthentication() {
        HttpHeaders headers = AuthTestSupport.jsonHeaders();
        headers.set("Idempotency-Key", "no-jwt-key-00001");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest("some-owner"), headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /orders rejects ownerId that does not match JWT subject")
    void createOrderRejectsOwnerMismatch() {
        seedCart("jwt-owner-a", 1);
        HttpHeaders headers = AuthTestSupport.bearerHeaders(jwtEncoder, securityProperties, "jwt-owner-a");
        headers.addAll(AuthTestSupport.jsonHeaders());
        headers.set("Idempotency-Key", "owner-mismatch-key01");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest("jwt-owner-b"), headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().path("title").asText()).isEqualTo("Forbidden");
    }

    @Test
    @DisplayName("POST /orders returns 429 when Redis rate limit is exceeded")
    void createOrderRateLimited() {
        String owner = "rate-limit-user";

        seedCart(owner, 1);
        assertThat(createWithKey(owner, "rate-limit-key-001").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        seedCart(owner, 1);
        assertThat(createWithKey(owner, "rate-limit-key-002").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        seedCart(owner, 1);
        ResponseEntity<JsonNode> limited = createWithKey(owner, "rate-limit-key-003");
        assertThat(limited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(limited.getBody().path("title").asText()).isEqualTo("Rate limit exceeded");
    }

    @Test
    @DisplayName("GET /products remains public without JWT")
    void catalogReadIsPublic() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/api/v1/products?size=1", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<JsonNode> createWithKey(String owner, String idempotencyKey) {
        HttpHeaders headers = AuthTestSupport.bearerHeaders(jwtEncoder, securityProperties, owner);
        headers.addAll(AuthTestSupport.jsonHeaders());
        headers.set("Idempotency-Key", idempotencyKey);
        return restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(new CreateOrderRequest(owner), headers),
                JsonNode.class);
    }

    private void seedCart(String owner, int quantity) {
        restTemplate.postForEntity(
                "/api/v1/carts/" + owner + "/items",
                new AddCartItemRequest(CLASSIC_BURGER_ID, quantity),
                JsonNode.class);
    }
}
