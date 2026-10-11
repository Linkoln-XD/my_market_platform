package com.market.notification.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaHealthIndicator implements HealthIndicator, SmartInitializingSingleton, DisposableBean {

    public static final String BROKER_UNAVAILABLE = "Kafka broker is not available";

    private final AdminClient adminClient;
    private final int timeoutSeconds;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        this.timeoutSeconds = Math.max(kafkaAdmin.getOperationTimeout(), 1);
    }

    @Override
    public void afterSingletonsInstantiated() {
        try {
            describeCluster();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(BROKER_UNAVAILABLE, ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException(BROKER_UNAVAILABLE, ex);
        }
    }

    @Override
    public Health health() {
        try {
            return Health.up().withDetail("clusterId", describeCluster()).build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Health.down(ex).build();
        } catch (ExecutionException | TimeoutException ex) {
            return Health.down(ex).build();
        }
    }

    private String describeCluster() throws InterruptedException, ExecutionException, TimeoutException {
        return adminClient.describeCluster()
                .clusterId()
                .get(timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        adminClient.close(Duration.ofSeconds(1));
    }
}
