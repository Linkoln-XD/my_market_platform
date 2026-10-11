package com.market.order.config;

import com.market.events.topics.CatalogTopics;
import com.market.events.topics.OrderTopics;
import com.market.events.topics.PaymentTopics;
import com.market.order.outbox.OutboxProperties;
import com.market.order.payment.PaymentMockProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties({OutboxProperties.class, PaymentMockProperties.class})
public class KafkaTopicConfig {

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(OrderTopics.ORDER_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic paymentEventsTopic() {
        return TopicBuilder.name(PaymentTopics.PAYMENT_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic catalogEventsTopic() {
        return TopicBuilder.name(CatalogTopics.CATALOG_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
