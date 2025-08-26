package com.merfonteen.likeservice.service.impl;

import com.merfonteen.likeservice.client.PostClient;
import com.merfonteen.likeservice.dto.LikeResponse;
import com.merfonteen.likeservice.kafka.eventProducer.LikeEventProducer;
import com.merfonteen.likeservice.model.Like;
import com.merfonteen.likeservice.repository.LikeRepository;
import com.merfonteen.likeservice.service.impl.config.RestTemplateConfig;
import com.merfonteen.likeservice.service.impl.redis.LikeRateLimiter;
import com.merfonteen.likeservice.service.impl.redis.RedisCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import static com.merfonteen.likeservice.service.impl.RedisCacheIntegrationTests.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(RestTemplateConfig.class)
public class RedisCacheIntegrationTests extends AbstractContainer {

    @MockBean
    private PostClient postClient;

    @Autowired
    private LikeRepository likeRepository;

    @MockBean
    private LikeRateLimiter likeRateLimiter;

    @MockBean
    private LikeEventProducer likeEventProducer;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private RedisCounter redisCounter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> redisOps;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        redisOps = redisTemplate.opsForValue();
    }

    @Test
    void testGetLikeCount_ShouldCacheResult() {
        Like like = getSavedLike();

        String url = buildLikeCountUrl(like.getPostId());
        testRestTemplate.getForEntity(url, Long.class);

        String cacheKey = buildLikeCountCacheKey(like.getPostId());
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    void testLikePost_ShouldIncrementLikeCountInRedis() {
        Like like = getSavedLike();
        String cacheKey = buildLikeCountCacheKey(like.getPostId());

        doNothing().when(postClient).checkPostExists(like.getPostId());

        String likeCountUrl = buildLikeCountUrl(like.getPostId());
        Long previousLikeCount = testRestTemplate.getForEntity(likeCountUrl, Long.class).getBody();
        assertThat(redisOps.get(cacheKey)).isEqualTo(String.valueOf(previousLikeCount));

        HttpHeaders httpHeaders = buildHttpHeaders(like.getUserId() + 1);
        HttpEntity<Long> request = new HttpEntity<>(like.getPostId(), httpHeaders);

        String likeOnPostsUrl = buildLikesOnPostsUrl(like.getPostId());
        testRestTemplate.postForEntity(likeOnPostsUrl, request, LikeResponse.class);

        String newValue = redisOps.get(cacheKey);
        assertThat(Long.parseLong(newValue)).isGreaterThan(previousLikeCount);
    }

    @Test
    void testRemoveLike_ShouldDecrementLikeCountInRedis() {
        Like like = getSavedLike();
        String cacheKey = buildLikeCountCacheKey(like.getPostId());

        doNothing().when(postClient).checkPostExists(like.getPostId());

        Long actualValue = 10L;
        stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(actualValue));

        HttpHeaders httpHeaders = buildHttpHeaders(like.getUserId());
        HttpEntity<Long> request = new HttpEntity<>(like.getPostId(), httpHeaders);

        String likesOnPostsUrl = buildLikesOnPostsUrl(like.getPostId());
        testRestTemplate.exchange(likesOnPostsUrl, HttpMethod.DELETE, request, LikeResponse.class);

        String newValue = redisOps.get(cacheKey);
        assertThat(Long.parseLong(newValue)).isLessThan(actualValue);
    }

    private Like getSavedLike() {
        long postId = ThreadLocalRandom.current().nextLong(1000, 9999);
        long userId = ThreadLocalRandom.current().nextLong(1000, 9999);
        return likeRepository.save(Like.builder()
                .userId(userId)
                .postId(postId)
                .createdAt(Instant.now())
                .build());
    }

    static class TestResources {
        static final String LIKE_COUNT_KEY = "like:count:post:";

        static String buildLikeCountUrl(Long postId) {
            return "/api/likes/posts/" + postId + "/count";
        }

        static String buildLikeCountCacheKey(Long postId) {
            return LIKE_COUNT_KEY + postId;
        }

        static String buildLikesOnPostsUrl(Long postId) {
            return "/api/likes/posts/" + postId;
        }

        static HttpHeaders buildHttpHeaders(Long userId) {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set("X-User-Id", String.valueOf(userId));
            return httpHeaders;
        }
    }
}
