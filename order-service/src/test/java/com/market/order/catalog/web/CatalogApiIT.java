package com.market.order.catalog.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.market.order.catalog.dto.UpdateProductRequest;
import com.market.order.catalog.dto.UpdateStockRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class CatalogApiIT {

    private static final UUID CLASSIC_BURGER_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID VEGGIE_BURGER_ID = UUID.fromString("22222222-2222-2222-2222-222222222202");
    private static final UUID BURGERS_CATEGORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");

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
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("seed data is exposed via catalog REST API")
    void seedDataIsAvailable() {
        ResponseEntity<JsonNode> categories = restTemplate.getForEntity("/api/v1/categories", JsonNode.class);
        assertThat(categories.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(categories.getBody()).isNotNull();
        assertThat(categories.getBody()).hasSize(3);

        ResponseEntity<JsonNode> product = restTemplate.getForEntity(
                "/api/v1/products/" + CLASSIC_BURGER_ID, JsonNode.class);
        assertThat(product.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(product.getBody()).isNotNull();
        assertThat(product.getBody().path("name").asText()).isEqualTo("Classic Burger");
        assertThat(product.getBody().path("stockQuantity").asInt()).isEqualTo(120);
        assertThat(product.getBody().path("categoryId").asText()).isEqualTo(BURGERS_CATEGORY_ID.toString());

        ResponseEntity<JsonNode> page = restTemplate.getForEntity(
                "/api/v1/products?categoryId=" + BURGERS_CATEGORY_ID + "&size=10", JsonNode.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody()).isNotNull();
        assertThat(page.getBody().path("totalElements").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("product update evicts Redis cache entry")
    void productUpdateEvictsCache() {
        String productUrl = "/api/v1/products/" + VEGGIE_BURGER_ID;
        assertThat(restTemplate.getForEntity(productUrl, JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.OK);

        Set<String> keysAfterRead = redisKeysForProduct(VEGGIE_BURGER_ID);
        assertThat(keysAfterRead).isNotEmpty();

        UpdateProductRequest update = new UpdateProductRequest(
                BURGERS_CATEGORY_ID,
                "Veggie Burger (updated)",
                "Plant patty, avocado, sprouts",
                799,
                "USD",
                true);
        ResponseEntity<JsonNode> updated = restTemplate.exchange(
                productUrl, HttpMethod.PUT, new HttpEntity<>(update), JsonNode.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().path("name").asText()).isEqualTo("Veggie Burger (updated)");

        Set<String> keysAfterUpdate = redisKeysForProduct(VEGGIE_BURGER_ID);
        assertThat(keysAfterUpdate).isEmpty();

        ResponseEntity<JsonNode> loadedAgain = restTemplate.getForEntity(productUrl, JsonNode.class);
        assertThat(loadedAgain.getBody().path("name").asText()).isEqualTo("Veggie Burger (updated)");
        assertThat(redisKeysForProduct(VEGGIE_BURGER_ID)).isNotEmpty();
    }

    @Test
    @DisplayName("stock can be adjusted through the API")
    void stockCanBeAdjusted() {
        String stockUrl = "/api/v1/products/" + CLASSIC_BURGER_ID + "/stock";
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                stockUrl,
                HttpMethod.PUT,
                new HttpEntity<>(new UpdateStockRequest(42)),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("stockQuantity").asInt()).isEqualTo(42);
    }

    private Set<String> redisKeysForProduct(UUID productId) {
        return stringRedisTemplate.keys("*").stream()
                .filter(key -> key.contains("catalog:product") && key.contains(productId.toString()))
                .collect(Collectors.toSet());
    }
}
