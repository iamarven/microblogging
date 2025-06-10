package com.merfonteen.commentservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisCacheCleaner {

    private final RedisTemplate<String, Object> redisTemplate;

    public void evictCommentCacheOnPostByPostId(Long postId) {
        String pattern = "comments-by-postId::" + postId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if(keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} cached pages for post '{}'", keys.size(), postId);
        }
    }

    public void evictCommentRepliesCacheByParentId(Long parentId) {
        String pattern = "comment-replies::" + parentId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if(keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} cached pages for comment '{}'", keys.size(), parentId);
        }
    }
}
