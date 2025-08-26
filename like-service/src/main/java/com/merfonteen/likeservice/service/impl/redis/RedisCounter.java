package com.merfonteen.likeservice.service.impl.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class RedisCounter {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_KEY = "like:count:post:";

    public String getCachedValue(Long postId) {
        return stringRedisTemplate.opsForValue().get(CACHE_KEY + postId);
    }

    public void setCounter(Long postId, Long count) {
        stringRedisTemplate.opsForValue().set(CACHE_KEY + postId, String.valueOf(count), Duration.ofMinutes(10));
    }

    public void incrementCounter(Long postId) {
        stringRedisTemplate.opsForValue().increment(CACHE_KEY + postId);
    }

    public void decrementCounter(Long postId) {
        if (stringRedisTemplate.hasKey(CACHE_KEY + postId)) {
            stringRedisTemplate.opsForValue().decrement(CACHE_KEY + postId);
        }
    }
}
