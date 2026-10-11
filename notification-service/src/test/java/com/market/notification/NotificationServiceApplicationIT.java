package com.market.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class NotificationServiceApplicationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"));

    @Container
    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.2"));

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("flyway creates notification schema tables")
    void flywayCreatesNotificationSchema() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            try (ResultSet tables = statement.executeQuery("""
                    select table_name
                    from information_schema.tables
                    where table_schema = 'notification'
                      and table_type = 'BASE TABLE'
                      and table_name not like 'flyway_%'
                    order by table_name
                    """)) {
                assertThat(tables.next()).isTrue();
                assertThat(tables.getString("table_name")).isEqualTo("delivered_notifications");
                assertThat(tables.next()).isTrue();
                assertThat(tables.getString("table_name")).isEqualTo("processed_events");
                assertThat(tables.next()).isFalse();
            }
        }
    }
}
