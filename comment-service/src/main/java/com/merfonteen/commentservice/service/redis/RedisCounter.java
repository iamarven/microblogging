package com.merfonteen.commentservice.service.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class RedisCounter {
    private static final String COMMENTS_CACHE_KEY = "comment:count:post:";
    private static final String REPLIES_CACHE_KEY = "replies:count:comment:";

    private final StringRedisTemplate stringRedisTemplate;

    public String getCommentsCacheKey(Long postId) {
        return COMMENTS_CACHE_KEY + postId;
    }

    public String getRepliesCacheKey(Long commentId) {
        return REPLIES_CACHE_KEY + commentId;
    }

    public String getCachedValue(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void setCounter(String key, Long count) {
        stringRedisTemplate.opsForValue().set(key, String.valueOf(count), Duration.ofMinutes(10));
    }

    public void incrementCounter(String key) {
        stringRedisTemplate.opsForValue().increment(key);
    }

    public void decrementCounter(String key) {
        if(stringRedisTemplate.hasKey(key)) {
            stringRedisTemplate.opsForValue().decrement(key);
        }
    }
}
