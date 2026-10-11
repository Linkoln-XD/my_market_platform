package com.market.order.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderCreateRateLimitService {

    private static final String KEY_PREFIX = "ratelimit:order-create:";

    private final StringRedisTemplate redisTemplate;
    private final MarketSecurityProperties securityProperties;

    public void assertWithinLimit(String userId) {
        Duration window = securityProperties.rateLimit().createOrderWindow();
        long windowSeconds = Math.max(window.getSeconds(), 1);
        long bucket = Instant.now().getEpochSecond() / windowSeconds;
        String key = KEY_PREFIX + userId + ":" + bucket;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        if (count != null && count > securityProperties.rateLimit().createOrderLimit()) {
            throw new RateLimitExceededException(
                    "Order creation rate limit exceeded (%d per %s)"
                            .formatted(
                                    securityProperties.rateLimit().createOrderLimit(),
                                    window));
        }
    }
}
