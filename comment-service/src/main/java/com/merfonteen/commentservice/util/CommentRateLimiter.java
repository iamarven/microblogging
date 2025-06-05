package com.merfonteen.commentservice.util;

import com.merfonteen.exceptions.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class CommentRateLimiter {

    private final Long MAX_COMMENTS = 5L;
    private final Duration DURATION = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;

    public void limitLeavingComments(Long userId) {
        String cacheKey = "limit:comment:user:" + userId;
        Long count = stringRedisTemplate.opsForValue().increment(cacheKey);

        if(count == 1) {
            stringRedisTemplate.expire(cacheKey, DURATION);
        }

        if(count > MAX_COMMENTS) {
            throw new TooManyRequestsException("You can leave max 5 comments per minute");
        }
    }
}
