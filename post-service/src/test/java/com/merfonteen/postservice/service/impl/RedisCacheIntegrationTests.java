package com.merfonteen.postservice.service.impl;

import com.merfonteen.postservice.abstractContainers.AbstractRedisIntegrationTest;
import com.merfonteen.postservice.client.UserClient;
import com.merfonteen.postservice.dto.PostCreateRequest;
import com.merfonteen.postservice.dto.PostResponse;
import com.merfonteen.postservice.dto.PostUpdateRequest;
import com.merfonteen.postservice.dto.UserPostsPageResponse;
import com.merfonteen.postservice.kafkaProducer.PostEventProducer;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.impl.config.RestTemplateConfig;
import org.jetbrains.annotations.NotNull;
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

    @Test
    void testGetPostById_ShouldCacheResult() {
        Post post = getSavedPost();

        String url = "/api/posts/" + post.getId();
        testRestTemplate.getForEntity(url, PostResponse.class);

        String cacheKey = "post-by-id::" + post.getId();
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    void testGetUserPosts_ShouldCacheResult() {
        Post post = getSavedPost();

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        String url = "/api/posts/users/" + post.getAuthorId();
        testRestTemplate.getForEntity(url, UserPostsPageResponse.class);

        String cacheKey = "user-posts::" + post.getAuthorId() + ":0:10";
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    void testCreatePost_ShouldEvictCache() {
        Post post = getSavedPost();

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        String cacheKey = "user-posts::" + post.getAuthorId() + ":0:10";

        String urlToCacheData = "/api/posts/users/" + post.getAuthorId();
        testRestTemplate.getForEntity(urlToCacheData, UserPostsPageResponse.class);
        assertThat(redisOps.get(cacheKey)).isNotNull();

        PostCreateRequest createDto = PostCreateRequest.builder()
                .content("new content")
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-User-Id", String.valueOf(post.getAuthorId()));

        HttpEntity<PostCreateRequest> request = new HttpEntity<>(createDto, httpHeaders);
        testRestTemplate.postForEntity("/api/posts", request, PostResponse.class);

        assertThat(redisOps.get(cacheKey)).isNull();
    }

    @Test
    void testUpdatePost_ShouldEvictCache() {
        Post post = getSavedPost();

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        String cacheKeyForPostById = "post-by-id::" + post.getId();
        String cacheKeyForUserPosts = "user-posts::" + post.getAuthorId() + ":0:10";

        String urlToCacheDataForPostById = "/api/posts/" + post.getId();
        testRestTemplate.getForEntity(urlToCacheDataForPostById, PostResponse.class);
        assertThat(redisOps.get(cacheKeyForPostById)).isNotNull();

        String urlToCacheDataForUserPosts = "/api/posts/users/" + post.getAuthorId();
        testRestTemplate.getForEntity(urlToCacheDataForUserPosts, UserPostsPageResponse.class);
        assertThat(redisOps.get(cacheKeyForUserPosts)).isNotNull();

        PostUpdateRequest updateDto = PostUpdateRequest.builder()
                .content("updated content")
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-User-Id", String.valueOf(post.getAuthorId()));

        HttpEntity<PostUpdateRequest> request = new HttpEntity<>(updateDto, httpHeaders);

        testRestTemplate.exchange(
                "/api/posts/" + post.getId(),
                HttpMethod.PATCH,
                request,
                PostResponse.class
        );

        assertThat(redisOps.get(cacheKeyForPostById)).isNull();
        assertThat(redisOps.get(cacheKeyForUserPosts)).isNull();
    }

    @Test
    void testDeletePost_ShouldEvictCache() {
        Post post = getSavedPost();

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        String cacheKeyForPostById = "post-by-id::" + post.getId();
        String cacheKeyForUserPosts = "user-posts::" + post.getAuthorId() + ":0:10";

        String urlToCacheDataForPostById = "/api/posts/" + post.getId();
        testRestTemplate.getForEntity(urlToCacheDataForPostById, PostResponse.class);
        assertThat(redisOps.get(cacheKeyForPostById)).isNotNull();

        String urlToCacheDataForUserPosts = "/api/posts/users/" + post.getAuthorId();
        testRestTemplate.getForEntity(urlToCacheDataForUserPosts, UserPostsPageResponse.class);
        assertThat(redisOps.get(cacheKeyForUserPosts)).isNotNull();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-User-Id", String.valueOf(post.getAuthorId()));

        HttpEntity<Void> request = new HttpEntity<>(httpHeaders);

        ResponseEntity<PostResponse> response = testRestTemplate.exchange(
                "/api/posts/" + post.getId(),
                HttpMethod.DELETE,
                request,
                PostResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(redisOps.get(cacheKeyForPostById)).isNull();
        assertThat(redisOps.get(cacheKeyForUserPosts)).isNull();
    }

    @NotNull
    private Post getSavedPost() {
        return postRepository.save(Post.builder()
                .content("some content")
                .authorId(1L)
                .createdAt(Instant.now())
                .build());
    }
}
