package com.merfonteen.postservice.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class StringRedisCounter {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_KEY = "post:count:user:";

    public String getCacheKey(Long userId) {
        return CACHE_KEY + userId;
    }

    public String getCachedValue(Long userId) {
        String key = getCacheKey(userId);
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void incrementCounter(Long userId) {
        String key = getCacheKey(userId);
        stringRedisTemplate.opsForValue().increment(key, 1);
    }

    public void decrementCounter(Long userId) {
        String key = getCacheKey(userId);
        if (stringRedisTemplate.hasKey(key)) {
            stringRedisTemplate.opsForValue().decrement(key, 1);
        }
    }

    public void putCounter(Long userId, Long numberOfPostsFromDb) {
        String key = getCacheKey(userId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(numberOfPostsFromDb), Duration.ofMinutes(10));
    }
}
