package com.merfonteen.feedservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class FeedCacheInvalidator {
    private final RedisTemplate<String, Object> redisTemplate;

    public void evictFeedCache(Set<Long> userIdsToEvictCache) {
        for (Long userId : userIdsToEvictCache) {
            String pattern = "feed::" + userId + ":*";
            Set<String> keys = new HashSet<>();
            try (Cursor<String> scan = redisTemplate.scan(ScanOptions.scanOptions()
                    .match(pattern)
                    .count(500)
                    .build())) {
                scan.forEachRemaining(keys::add);
            } catch (Exception e) {
                log.error("Error while evicting cache for userId '{}': {}", userId, e.getMessage());
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} cached pages for user '{}'", keys.size(), userId);
            }
        }
    }

    public void evictFeedCache(Long userId) {

    }
}
