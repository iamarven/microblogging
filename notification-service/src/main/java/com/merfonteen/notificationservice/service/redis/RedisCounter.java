package com.merfonteen.notificationservice.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class RedisCounter {
    private static final String UNREAD_NOTIFICATIONS_CACHE_KEY = "user:notifications:unread:count:";

    private final StringRedisTemplate stringRedisTemplate;

    public String getCachedValue(Long userId) {
        return stringRedisTemplate.opsForValue().get(buildCacheKey(userId));
    }

    public void refreshCountUnreadNotifications(Long userId) {
        stringRedisTemplate.opsForValue().set(UNREAD_NOTIFICATIONS_CACHE_KEY + userId, "0");
    }

    public void setCounter(Long userId, Long count) {
        stringRedisTemplate.opsForValue().set(buildCacheKey(userId), String.valueOf(count), Duration.ofMinutes(10));
    }

    public void incrementCounter(Long userId) {
        stringRedisTemplate.opsForValue().increment(buildCacheKey(userId));
    }

    public void decrementCounter(Long userId) {
        String key = buildCacheKey(userId);
        if(stringRedisTemplate.hasKey(key)) {
            stringRedisTemplate.opsForValue().decrement(key);
        }
    }

    private String buildCacheKey(Long userId) {
        return  UNREAD_NOTIFICATIONS_CACHE_KEY + userId;
    }
}
