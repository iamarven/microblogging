package com.merfonteen.feedservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class FeedCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictFeedCache(Set<Long> userIdsToEvictCache) {
        for(Long userId : userIdsToEvictCache) {
            String pattern = "feed::" + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if(keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} cached pages for user '{}'", keys.size(), userId);
            }
        }
    }
}
