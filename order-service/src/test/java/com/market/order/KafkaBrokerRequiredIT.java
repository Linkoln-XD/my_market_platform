package com.market.order;

import com.market.order.kafka.KafkaHealthIndicator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Profile {@code test} uses the same fail-fast Kafka admin check as {@code local}.
 * Postgres and Redis are up so a missing broker is the reason startup stops.
 */
@Testcontainers
class KafkaBrokerRequiredIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Test
    @DisplayName("the process does not start when Kafka is unreachable")
    void applicationFailsToStartWhenKafkaIsUnreachable() {
        SpringApplication application = new SpringApplication(OrderServiceApplication.class);
        application.setAdditionalProfiles("test");

        Throwable thrown = catchThrowable(() -> application.run(
                "--server.port=0",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--spring.data.redis.host=" + REDIS.getHost(),
                "--spring.data.redis.port=" + REDIS.getMappedPort(6379),
                "--spring.kafka.bootstrap-servers=127.0.0.1:1",
                "--spring.kafka.admin.fail-fast=true",
                "--spring.kafka.admin.operation-timeout=3s",
                "--spring.kafka.properties.request.timeout.ms=1000",
                "--spring.kafka.properties.default.api.timeout.ms=2000",
                "--spring.kafka.properties.socket.connection.setup.timeout.ms=500",
                "--spring.kafka.properties.socket.connection.setup.timeout.max.ms=500"
        ));

        assertThat(thrown).isNotNull();
        assertThat(causeChain(thrown)).contains(KafkaHealthIndicator.BROKER_UNAVAILABLE);
    }

    private static String causeChain(Throwable throwable) {
        StringBuilder chain = new StringBuilder();
        Throwable current = throwable;
        while (current != null && chain.length() < 8000) {
            chain.append(current.getClass().getName())
                    .append(": ")
                    .append(current.getMessage())
                    .append('\n');
            current = current.getCause();
        }
        return chain.toString();
    }
}
