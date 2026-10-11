package com.market.notification.config;

import com.market.events.topics.OrderTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic orderEventsDlqTopic(NotificationProperties properties) {
        return TopicBuilder.name(properties.dlqTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(OrderTopics.ORDER_EVENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
