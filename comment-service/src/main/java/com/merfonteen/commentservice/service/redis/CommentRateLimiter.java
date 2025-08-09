package com.merfonteen.commentservice.service.redis;

import com.merfonteen.exceptions.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class CommentRateLimiter {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_KEY = "limit:comment:user:";
    private static final int MAX_COMMENTS = 5;
    private static final Duration DURATION = Duration.ofMinutes(1);

    public void limitLeavingComments(Long userId) {
        String key = CACHE_KEY + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);

        if(count != null && count == 1) {
            stringRedisTemplate.expire(key, DURATION);
        }

        if(count != null && count > MAX_COMMENTS) {
            throw new TooManyRequestsException("You can leave max 5 comments per minute");
        }
    }
}
