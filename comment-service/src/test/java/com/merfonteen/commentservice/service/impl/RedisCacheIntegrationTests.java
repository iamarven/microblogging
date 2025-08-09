package com.merfonteen.commentservice.service.impl;

import com.merfonteen.commentservice.client.PostClient;
import com.merfonteen.commentservice.dto.CommentCreateRequest;
import com.merfonteen.commentservice.dto.CommentResponse;
import com.merfonteen.commentservice.kafka.eventProducer.CommentEventProducer;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.repository.CommentRepository;
import com.merfonteen.commentservice.service.impl.config.RestTemplateConfig;
import com.merfonteen.commentservice.service.redis.CommentRateLimiter;
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
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(RestTemplateConfig.class)
@Testcontainers
public class RedisCacheIntegrationTests {

    @MockBean
    private PostClient postClient;

    @Autowired
    private CommentRepository commentRepository;

    @MockBean
    private CommentRateLimiter commentRateLimiter;

    @MockBean
    private CommentEventProducer commentEventProducer;

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
    void testGetCommentCount_ShouldCacheResult() {
        Comment comment = getSavedComment();

        String url = "/api/comments/posts/" + comment.getPostId() + "/count";
        testRestTemplate.getForEntity(url, Long.class);

        String cacheKey = "comment:count:post:" + comment.getPostId();
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    void testCreateComment_ShouldIncrementCommentCountInRedis() {
        Comment comment = getSavedComment();
        String cacheKey = "comment:count:post:" + comment.getPostId();

        doNothing().when(postClient).checkPostExists(comment.getPostId());

        String url1 = "/api/comments/posts/" + comment.getPostId() + "/count";
        Long previousCount = testRestTemplate.getForEntity(url1, Long.class).getBody();
        assertThat(redisOps.get(cacheKey)).isEqualTo(String.valueOf(previousCount));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-User-Id", String.valueOf(comment.getUserId() + 1));

        CommentCreateRequest requestDto = CommentCreateRequest.builder()
                .postId(comment.getPostId())
                .content("new comment")
                .build();

        HttpEntity<CommentCreateRequest> request = new HttpEntity<>(requestDto, httpHeaders);

        testRestTemplate.postForEntity("/api/comments", request, CommentResponse.class);

        String newValue = redisOps.get(cacheKey);
        assertThat(Long.parseLong(newValue)).isGreaterThan(previousCount);
    }

    @Test
    void testDeleteComment_ShouldDecrementCommentCountInRedis() {
        Comment comment = getSavedComment();
        String cacheKey = "comment:count:post:" + comment.getPostId();

        String url1 = "/api/comments/posts/" + comment.getPostId() + "/count";
        Long previousCount = testRestTemplate.getForEntity(url1, Long.class).getBody();
        assertThat(redisOps.get(cacheKey)).isEqualTo(String.valueOf(previousCount));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", String.valueOf(comment.getUserId()));

        HttpEntity<Void> request = new HttpEntity<>(headers);
        testRestTemplate.exchange("/api/comments/" + comment.getId(),
                HttpMethod.DELETE,
                request,
                CommentResponse.class);

        String newValue = redisOps.get(cacheKey);
        assertThat(Long.parseLong(newValue)).isLessThan(previousCount);
    }

    private Comment getSavedComment() {
        long postId = ThreadLocalRandom.current().nextLong(1000, 9999);
        long userId = ThreadLocalRandom.current().nextLong(1000, 9999);
        return commentRepository.save(Comment.builder()
                .userId(userId)
                .postId(postId)
                .content("text")
                .createdAt(Instant.now())
                .build());
    }
}
