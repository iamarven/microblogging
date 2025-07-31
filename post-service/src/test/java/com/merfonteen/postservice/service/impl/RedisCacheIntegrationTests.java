package com.merfonteen.postservice.service.impl;

import com.merfonteen.postservice.abstractContainers.AbstractRedisIntegrationTest;
import com.merfonteen.postservice.client.UserClient;
import com.merfonteen.postservice.dto.PostCreateRequest;
import com.merfonteen.postservice.dto.PostResponse;
import com.merfonteen.postservice.dto.PostUpdateRequest;
import com.merfonteen.postservice.dto.UserPostsPageResponse;
import com.merfonteen.postservice.kafka.PostEventProducer;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.impl.config.RestTemplateConfig;
import org.jetbrains.annotations.NotNull;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Objects;

import static com.merfonteen.postservice.service.impl.RedisCacheIntegrationTests.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(RestTemplateConfig.class)
public class RedisCacheIntegrationTests extends AbstractRedisIntegrationTest {

    @MockBean
    private UserClient userClient;

    @MockBean
    private PostEventProducer postEventProducer;

    @Autowired
    private PostRepository postRepository;

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
    void testGetPostById_ShouldCacheResult() {
        Post post = getSavedPost();

        testRestTemplate.getForEntity(POSTS_URL + post.getId(), PostResponse.class);

        assertThat(redisOps.get(buildPostByIdCacheKey(post.getId()))).isNotNull();
    }

    @Test
    void testGetUserPosts_ShouldCacheResult() {
        Post post = getSavedPost();

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        testRestTemplate.getForEntity(USER_POSTS_URL + post.getAuthorId(), UserPostsPageResponse.class);
        assertThat(redisOps.get(buildUserPostsCacheKey(post.getAuthorId()))).isNotNull();
    }

    @Test
    void testCreatePost_ShouldEvictCache() {
        Post post = getSavedPost();

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        testRestTemplate.getForEntity(USER_POSTS_URL + post.getAuthorId(), UserPostsPageResponse.class);
        assertThat(redisOps.get(buildUserPostsCacheKey(post.getAuthorId()))).isNotNull();

        HttpHeaders httpHeaders = buildHttpHeaders(post.getAuthorId());
        HttpEntity<PostCreateRequest> request = new HttpEntity<>(buildPostCreateRequest(), httpHeaders);
        testRestTemplate.postForEntity("/api/posts", request, PostResponse.class);

        assertThat(redisOps.get(buildUserPostsCacheKey(post.getAuthorId()))).isNull();
    }

    @Test
    void testUpdatePost_ShouldEvictCache() {
        Post post = getSavedPost();
        String postByIdCacheKey = buildPostByIdCacheKey(post.getId());
        String userPostsCacheKey = buildUserPostsCacheKey(post.getAuthorId());

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        testRestTemplate.getForEntity(POSTS_URL + post.getId(), PostResponse.class);
        assertThat(redisOps.get(postByIdCacheKey)).isNotNull();

        testRestTemplate.getForEntity(USER_POSTS_URL + post.getAuthorId(), UserPostsPageResponse.class);
        assertThat(redisOps.get(userPostsCacheKey)).isNotNull();

        HttpHeaders httpHeaders = buildHttpHeaders(post.getAuthorId());
        HttpEntity<PostUpdateRequest> request = new HttpEntity<>(buildPostUpdateRequest(), httpHeaders);
        testRestTemplate.exchange(
                "/api/posts/" + post.getId(),
                HttpMethod.PATCH,
                request,
                PostResponse.class
        );

        assertThat(redisOps.get(postByIdCacheKey)).isNull();
        assertThat(redisOps.get(userPostsCacheKey)).isNull();
    }

    @Test
    void testDeletePost_ShouldEvictCache() {
        Post post = getSavedPost();
        String postByIdCacheKey = buildPostByIdCacheKey(post.getId());
        String userPostsCacheKey = buildUserPostsCacheKey(post.getAuthorId());

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        testRestTemplate.getForEntity(POSTS_URL + post.getId(), PostResponse.class);
        assertThat(redisOps.get(postByIdCacheKey)).isNotNull();

        testRestTemplate.getForEntity(USER_POSTS_URL + post.getAuthorId(), UserPostsPageResponse.class);
        assertThat(redisOps.get(userPostsCacheKey)).isNotNull();

        HttpHeaders httpHeaders = buildHttpHeaders(post.getAuthorId());
        HttpEntity<Void> request = new HttpEntity<>(httpHeaders);
        ResponseEntity<PostResponse> response = testRestTemplate.exchange(
                "/api/posts/" + post.getId(),
                HttpMethod.DELETE,
                request,
                PostResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(redisOps.get(postByIdCacheKey)).isNull();
        assertThat(redisOps.get(userPostsCacheKey)).isNull();
    }

    @NotNull
    private Post getSavedPost() {
        return postRepository.save(Post.builder()
                .content(CONTENT)
                .authorId(AUTHOR_ID)
                .createdAt(DATE_CREATION)
                .build());
    }

    static class TestResources {
        static final Long AUTHOR_ID = 1L;
        static final String HTTP_HEADER_USER_ID = "X-User-Id";
        static final String POSTS_URL = "/api/posts/";
        static final String USER_POSTS_URL = "/api/posts/users/";
        static final String POST_BY_ID_CACHE_KEY = "post-by-id::";
        static final String USER_POSTS_CACHE_KEY = "user-posts::";
        static final String CONTENT = "Test content for post";
        static final Instant DATE_CREATION = Instant.now();
    }

    static String buildUserPostsCacheKey(Long postAuthorId) {
        return USER_POSTS_CACHE_KEY + postAuthorId;
    }

    static String buildPostByIdCacheKey(Long postId) {
        return POST_BY_ID_CACHE_KEY + postId;
    }


    static PostCreateRequest buildPostCreateRequest() {
        return PostCreateRequest.builder()
                .content(CONTENT)
                .build();
    }

    static PostUpdateRequest buildPostUpdateRequest() {
        return PostUpdateRequest.builder()
                .content(CONTENT)
                .build();
    }

    static HttpHeaders buildHttpHeaders(Long postAuthorId) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(HTTP_HEADER_USER_ID, String.valueOf(postAuthorId));
        return httpHeaders;
    }
}
