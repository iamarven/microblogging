package com.merfonteen.likeservice.service.impl;

import com.merfonteen.likeservice.client.PostClient;
import com.merfonteen.likeservice.dto.LikeDto;
import com.merfonteen.likeservice.kafkaProducer.LikeEventProducer;
import com.merfonteen.likeservice.model.Like;
import com.merfonteen.likeservice.repository.LikeRepository;
import com.merfonteen.likeservice.service.impl.config.RestTemplateConfig;
import com.merfonteen.likeservice.util.LikeRateLimiter;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(RestTemplateConfig.class)
@Testcontainers
public class RedisCacheIntegrationTests {

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
    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> redisOps;

    @BeforeEach
    void setUp() {
        redisOps = redisTemplate.opsForValue();
    }

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2.4")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Test
    void testGetLikeCount_ShouldCacheResult() {
        Like like = getSavedLike();

        String url = "/api/likes/posts/" + like.getPostId() + "/count";
        testRestTemplate.getForEntity(url, Long.class);

        String cacheKey = "like:count:post:" + like.getPostId();
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    void testLikePost_ShouldIncrementLikeCountInRedis() {
        Like like = getSavedLike();
        String cacheKey = "like:count:post:" + like.getPostId();

        doNothing().when(postClient).checkPostExists(like.getPostId());

        String url1 = "/api/likes/posts/" + like.getPostId() + "/count";
        Long previousLikeCount = testRestTemplate.getForEntity(url1, Long.class).getBody();
        assertThat(redisOps.get(cacheKey)).isEqualTo(String.valueOf(previousLikeCount));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-User-Id", String.valueOf(like.getUserId() + 1));

        HttpEntity<Long> request = new HttpEntity<>(like.getPostId(), httpHeaders);

        String url2 = "/api/likes/posts/" + like.getPostId();
        testRestTemplate.postForEntity(url2, request, LikeDto.class);

        String newValue = redisOps.get(cacheKey);
        assertThat(Long.parseLong(newValue)).isGreaterThan(previousLikeCount);
    }

    @Test
    void testRemoveLike_ShouldDecrementLikeCountInRedis() {
        Like like = getSavedLike();
        String cacheKey = "like:count:post:" + like.getPostId();

        doNothing().when(postClient).checkPostExists(like.getPostId());

        String url1 = "/api/likes/posts/" + like.getPostId() + "/count";
        Long previousLikeCount = testRestTemplate.getForEntity(url1, Long.class).getBody();
        assertThat(redisOps.get(cacheKey)).isEqualTo(String.valueOf(previousLikeCount));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-User-Id", String.valueOf(like.getUserId()));

        HttpEntity<Long> request = new HttpEntity<>(like.getPostId(), httpHeaders);

        String url2 = "/api/likes/posts/" + like.getPostId();
        testRestTemplate.exchange(
                "/api/likes/posts/" + like.getPostId(),
                HttpMethod.DELETE,
                request,
                LikeDto.class);

        String newValue = redisOps.get(cacheKey);
        assertThat(Long.parseLong(newValue)).isLessThan(previousLikeCount);
    }

    private Like getSavedLike() {
        return likeRepository.save(Like.builder()
                .userId(100L)
                .postId(5L)
                .createdAt(Instant.now())
                .build());
    }
}
