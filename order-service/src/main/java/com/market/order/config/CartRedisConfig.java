package com.market.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.order.cart.CartProperties;
import com.market.order.cart.model.CartDocument;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableConfigurationProperties(CartProperties.class)
public class CartRedisConfig {

    @Bean
    RedisTemplate<String, CartDocument> cartRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<CartDocument> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper.copy(), CartDocument.class);
        RedisTemplate<String, CartDocument> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
