package com.merfonteen.commentservice.service.redis;

import com.merfonteen.commentservice.config.CacheNames;
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

    public void evictPostsCache(Long postId) {
        String pattern = CacheNames.COMMENTS_BY_POST_ID_CACHE + "::" + postId + ":*";
        evictCache(pattern, "post", postId);
    }

    public void evictRepliesCache(Long parentId) {
        String pattern = CacheNames.COMMENT_REPLIES_CACHE + "::" + parentId + ":*";
        evictCache(pattern, "reply", parentId);
    }

    private void evictCache(String pattern, String entityName, Long entityId) {
        Set<String> keys = new HashSet<>();
        try (Cursor<String> scan = redisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern)
                .count(500)
                .build())) {
            scan.forEachRemaining(keys::add);
        } catch (Exception e) {
            log.error("Error while evicting cache for {} '{}': {}", entityName, entityId, e.getMessage());
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} cached pages for {} '{}'", keys.size(), entityName, entityId);
        }
    }
}
