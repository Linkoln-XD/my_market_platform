package com.market.order.cart.store;

import com.market.order.cart.CartProperties;
import com.market.order.cart.model.CartDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CartRedisStore {

    private final RedisTemplate<String, CartDocument> cartRedisTemplate;
    private final CartProperties cartProperties;

    public Optional<CartDocument> find(String ownerId) {
        return Optional.ofNullable(cartRedisTemplate.opsForValue().get(key(ownerId)));
    }

    public void save(CartDocument cart) {
        cart.setUpdatedAt(Instant.now());
        cartRedisTemplate.opsForValue().set(key(cart.getOwnerId()), cart, cartProperties.ttl());
    }

    public void delete(String ownerId) {
        cartRedisTemplate.delete(key(ownerId));
    }

    public Duration ttl(String ownerId) {
        Long seconds = cartRedisTemplate.getExpire(key(ownerId));
        if (seconds == null || seconds < 0) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds(seconds);
    }

    private String key(String ownerId) {
        return cartProperties.redisKeyPrefix() + ":" + ownerId;
    }
}
