package com.merfonteen.profileservice.service;

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
public class CacheInvalidator {
    private final RedisTemplate<String, Object> redisTemplate;

    public void invalidateCacheByEntityId(String cacheName, Long entityId) {
        Set<String> keys = new HashSet<>();
        String pattern = cacheName + "::" + entityId + ":*";
        try (Cursor<String> scan = redisTemplate.scan(ScanOptions.scanOptions()
                        .match(pattern)
                        .count(500)
                        .build())) {
            scan.forEachRemaining(keys::add);
        } catch (Exception ignored) {
            log.error("Error while invalidating cache={}, entityId={}", cacheName, entityId);
        }
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} cached pages for entityId={}", keys.size(), entityId);
        }
    }

}
