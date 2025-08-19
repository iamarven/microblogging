package com.merfonteen.postservice.service.redis;

import com.merfonteen.postservice.config.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisCacheInvalidator {
    private final RedisTemplate<String, Object> redisTemplate;

    public void evictUserPostsCache(Long userId) {
        String pattern = CacheNames.USER_POSTS + userId + "*:";
        Set<String> keys = new HashSet<>();
        try (Cursor<String> scan = redisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern)
                .count(500)
                .build())) {
            scan.forEachRemaining(keys::add);
        } catch (Exception e) {
            log.error("Error while evicting cache for user '{}': {}", userId, e.getMessage());
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} cached pages for '{}'", keys.size(), userId);
        }
    }
}
