package com.merfonteen.likeservice.util;

import com.merfonteen.exceptions.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Component
public class LikeRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_LIKES = 10;
    private static final Duration DURATION = Duration.ofSeconds(5);

    public void limitAmountOfLikes(Long userId) {
        String key = "like-limiter::user::" + userId;
        limit(key, "like", userId);
    }

    public void limitAmountOfUnlikes(Long userId) {
        String key = "unlike-limiter::user::" + userId;
        limit(key, "unlike", userId);
    }

    private void limit(String key, String actionLabel, Long userId) {
        Long count = redisTemplate.opsForValue().increment(key);

        if(count == 1) {
            redisTemplate.expire(key, DURATION);
        }

        if(count > MAX_LIKES) {
            log.warn("Too many likes per {}s for user '{}'", DURATION.getSeconds(), userId);
            throw new TooManyRequestsException("You have exceeded the allowed number of " + actionLabel + "s");
        }
    }
}
