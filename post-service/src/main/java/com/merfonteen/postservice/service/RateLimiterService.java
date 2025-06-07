package com.merfonteen.postservice.service;

import com.merfonteen.exceptions.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int POST_LIMIT = 5;
    private static final Duration DURATION = Duration.ofMinutes(1);

    public void validatePostCreationLimit(Long userId) {
        String key = "post_create:" + userId;
        Long currentCount = stringRedisTemplate.opsForValue().increment(key);

        if(currentCount == 1) {
            stringRedisTemplate.expire(key, DURATION);
        }

        if(currentCount > POST_LIMIT) {
            throw new TooManyRequestsException("You have exceeded the allowed number of posts per minute");
        }
    }
}
