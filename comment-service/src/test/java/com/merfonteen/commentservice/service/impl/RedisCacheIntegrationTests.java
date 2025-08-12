package com.merfonteen.commentservice.service.impl;

import com.merfonteen.commentservice.client.PostClient;
import com.merfonteen.commentservice.dto.CommentCreateRequest;
import com.merfonteen.commentservice.dto.CommentResponse;
import com.merfonteen.commentservice.kafka.eventProducer.CommentEventProducer;
import com.merfonteen.commentservice.mapper.CommentMapper;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.repository.CommentRepository;
import com.merfonteen.commentservice.service.impl.config.RestTemplateConfig;
import com.merfonteen.commentservice.service.redis.CommentRateLimiter;
import com.merfonteen.commentservice.service.redis.RedisCounter;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static com.merfonteen.commentservice.service.impl.RedisCacheIntegrationTests.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(RestTemplateConfig.class)
public class RedisCacheIntegrationTests extends AbstractContainer {

    @MockBean
    private PostClient postClient;

    @Autowired
    private RedisCounter redisCounter;

    @MockBean
    private CommentMapper commentMapper;

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

    @AfterEach
    void tearDown() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushDb();
    }

    @Test
    void testGetCommentCount_ShouldCacheResult() {
        Comment comment = getSavedComment();

        String url = buildCommentCountUrl(comment.getPostId());
        testRestTemplate.getForEntity(url, Long.class);

        String cacheKey = COMMENT_COUNT_CACHE_KEY + comment.getPostId();
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    void testCreateComment_ShouldIncrementCommentCountInRedis() {
        Comment comment = getSavedComment();
        String cacheKey = COMMENT_COUNT_CACHE_KEY + comment.getPostId();

        doNothing().when(postClient).checkPostExists(comment.getPostId());

        String url1 = buildCommentCountUrl(comment.getPostId());
        Long previousCount = testRestTemplate.getForEntity(url1, Long.class).getBody();
        assertThat(redisOps.get(cacheKey)).isEqualTo(String.valueOf(previousCount));

        CommentCreateRequest requestDto = buildCommentCreateRequest(comment);
        HttpEntity<CommentCreateRequest> request = new HttpEntity<>(requestDto, buildHttpHeaders(comment));

        testRestTemplate.postForEntity(COMMENTS_URL, request, CommentResponse.class);

        String newValue = redisOps.get(cacheKey);
        assertThat(Long.parseLong(newValue)).isGreaterThan(previousCount);
    }

    @Test
    void testDeleteComment_ShouldDecrementCommentCountInRedis() {
        Comment comment = getSavedComment();
        String cacheKey = COMMENT_COUNT_CACHE_KEY + comment.getPostId();

        String url1 = buildCommentCountUrl(comment.getPostId());
        Long previousCount = testRestTemplate.getForEntity(url1, Long.class).getBody();
        assertThat(redisOps.get(cacheKey)).isEqualTo(String.valueOf(previousCount));

        HttpEntity<Void> request = new HttpEntity<>(buildHttpHeaders(comment));
        testRestTemplate.exchange(COMMENTS_URL_WITH_ID + comment.getId(),
                HttpMethod.DELETE,
                request,
                Void.class);

        String newValue = redisOps.get(cacheKey);
        assertThat(Long.parseLong(newValue)).isLessThan(previousCount);
    }

    private Comment getSavedComment() {
        long postId = ThreadLocalRandom.current().nextLong(1000, 999999);
        long userId = ThreadLocalRandom.current().nextLong(1000, 999999);
        return commentRepository.save(Comment.builder()
                .userId(userId)
                .postId(postId)
                .content("text")
                .createdAt(Instant.now())
                .build());
    }

    static class TestResources {
        static final String COMMENTS_URL = "/api/comments";
        static final String COMMENTS_URL_WITH_ID = "/api/comments/";
        static final String COMMENT_COUNT_CACHE_KEY = "comment:count:post:";

        static String buildCommentCountUrl(Long postId) {
            return "/api/comments/posts/" + postId + "/count";
        }

        static HttpHeaders buildHttpHeaders(Comment comment) {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set("X-User-Id", String.valueOf(comment.getUserId()));
            return httpHeaders;
        }

        static CommentCreateRequest buildCommentCreateRequest(Comment comment) {
            return CommentCreateRequest.builder()
                    .postId(comment.getPostId())
                    .content("new comment")
                    .build();
        }
    }
}
