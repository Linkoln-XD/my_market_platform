package com.market.order;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The service starts with real Postgres, Redis, and Kafka.
 * Messaging substitutes are not part of the test profile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OrderServiceApplicationIT {

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

    @Test
    @DisplayName("actuator health is UP for postgres, redis, and kafka")
    void actuatorHealthIsUp() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().path("components").path("db").path("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().path("components").path("redis").path("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().path("components").path("kafka").path("status").asText()).isEqualTo("UP");
    }

    @Test
    @DisplayName("readiness includes the kafka dependency")
    void readinessIncludesKafka() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health/readiness", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().path("components").path("kafka").path("status").asText()).isEqualTo("UP");
    }

    @Test
    @DisplayName("flyway applies catalog migrations including seed data")
    void flywayAppliesCatalogMigrations() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet history = statement.executeQuery(
                    "select version, success from flyway_schema_history order by installed_rank")) {
                assertThat(history.next()).isTrue();
                assertThat(history.getString("version")).isEqualTo("1");
                assertThat(history.getBoolean("success")).isTrue();
                assertThat(history.next()).isTrue();
                assertThat(history.getString("version")).isEqualTo("2");
                assertThat(history.next()).isTrue();
                assertThat(history.getString("version")).isEqualTo("3");
                assertThat(history.next()).isFalse();
            }
            try (ResultSet products = statement.executeQuery("select count(*) from catalog.products")) {
                assertThat(products.next()).isTrue();
                assertThat(products.getInt(1)).isEqualTo(4);
            }
            try (ResultSet schemas = statement.executeQuery(
                    """
                    select schema_name
                    from information_schema.schemata
                    where schema_name in ('catalog', 'ordering')
                    """)) {
                Set<String> names = new HashSet<>();
                while (schemas.next()) {
                    names.add(schemas.getString("schema_name"));
                }
                assertThat(names).containsExactlyInAnyOrder("catalog", "ordering");
            }
        }
    }
}
