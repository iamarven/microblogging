package com.merfonteen.profileservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merfonteen.dtos.PublicUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Component
public class CacheService {
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

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
}
