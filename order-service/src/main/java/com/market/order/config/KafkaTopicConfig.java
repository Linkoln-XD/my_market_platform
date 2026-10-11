package com.market.order.config;

import com.market.events.topics.OrderTopics;
import com.market.order.outbox.OutboxProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class KafkaTopicConfig {

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(OrderTopics.ORDER_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
