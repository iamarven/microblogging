package com.merfonteen.profileservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merfonteen.dtos.PublicUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
@Component
public class CacheService {
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public PublicUserDto getOrLoad(Long userId, Supplier<PublicUserDto> loader) {
        String cacheKey = "user::" + userId;
        return getOrLoad(cacheKey, Duration.ofSeconds(120), PublicUserDto.class, loader);
    }

    public <T> T getOrLoad(String cacheKey, Duration ttl, Class<T> type, Supplier<T> loader) {
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            try {
                return objectMapper.readValue(cachedValue, type);
            } catch (Exception ignored) {}
        }

        T value = loader.get();
        if (value != null) {
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(value), ttl);
            } catch (Exception ignored) {}
        }

        return value;
    }

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
