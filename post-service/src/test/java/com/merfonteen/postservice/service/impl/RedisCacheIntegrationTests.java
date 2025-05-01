package com.merfonteen.postservice.service.impl;

import com.merfonteen.postservice.client.UserClient;
import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.dto.PostUpdateDto;
import com.merfonteen.postservice.dto.UserPostsPageResponseDto;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.PostCacheService;
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
import org.springframework.http.*;
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
@Testcontainers
@Import(RestTemplateConfig.class)
public class RedisCacheIntegrationTests {

    @MockBean
    private UserClient userClient;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCacheService postCacheService;

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
    void testGetPostById_ShouldCacheResult() {
        Post post = getSavedPost();

        String url = "/api/posts/" + post.getId();
        testRestTemplate.getForEntity(url, PostResponseDto.class);

        String cacheKey = "post-by-id::" + post.getId();
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    void testGetUserPosts_ShouldCacheResult() {
        Post post = getSavedPost();

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        String url = "/api/posts/users/" + post.getAuthorId();
        testRestTemplate.getForEntity(url, UserPostsPageResponseDto.class);

        String cacheKey = "user-posts::" + post.getAuthorId() + ":0:10";
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    void testCreatePost_ShouldEvictCache() {
        Post post = getSavedPost();

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        String cacheKey = "user-posts::" + post.getAuthorId() + ":0:10";

        String urlToCacheData = "/api/posts/users/" + post.getAuthorId();
        testRestTemplate.getForEntity(urlToCacheData, UserPostsPageResponseDto.class);
        assertThat(redisOps.get(cacheKey)).isNotNull();

        PostCreateDto createDto = PostCreateDto.builder()
                .content("new content")
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-User-Id", String.valueOf(post.getAuthorId()));

        HttpEntity<PostCreateDto> request = new HttpEntity<>(createDto, httpHeaders);
        testRestTemplate.postForEntity("/api/posts", request, PostResponseDto.class);

        assertThat(redisOps.get(cacheKey)).isNull();
    }

    @Test
    void testUpdatePost_ShouldEvictCache() {
        Post post = getSavedPost();

        doNothing().when(userClient).checkUserExists(post.getAuthorId());

        String cacheKeyForPostById = "post-by-id::" + post.getId();
        String cacheKeyForUserPosts = "user-posts::" + post.getAuthorId() + ":0:10";

        String urlToCacheDataForPostById = "/api/posts/" + post.getId();
        testRestTemplate.getForEntity(urlToCacheDataForPostById, PostResponseDto.class);
        assertThat(redisOps.get(cacheKeyForPostById)).isNotNull();

        String urlToCacheDataForUserPosts = "/api/posts/users/" + post.getAuthorId();
        testRestTemplate.getForEntity(urlToCacheDataForUserPosts, UserPostsPageResponseDto.class);
        assertThat(redisOps.get(cacheKeyForUserPosts)).isNotNull();

        PostUpdateDto updateDto = PostUpdateDto.builder()
                .content("updated content")
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-User-Id", String.valueOf(post.getAuthorId()));

        HttpEntity<PostUpdateDto> request = new HttpEntity<>(updateDto, httpHeaders);

        testRestTemplate.exchange(
                "/api/posts/" + post.getId(),
                HttpMethod.PATCH,
                request,
                PostResponseDto.class
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
        testRestTemplate.getForEntity(urlToCacheDataForPostById, PostResponseDto.class);
        assertThat(redisOps.get(cacheKeyForPostById)).isNotNull();

        String urlToCacheDataForUserPosts = "/api/posts/users/" + post.getAuthorId();
        testRestTemplate.getForEntity(urlToCacheDataForUserPosts, UserPostsPageResponseDto.class);
        assertThat(redisOps.get(cacheKeyForUserPosts)).isNotNull();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set("X-User-Id", String.valueOf(post.getAuthorId()));

        HttpEntity<Void> request = new HttpEntity<>(httpHeaders);

        ResponseEntity<PostResponseDto> response = testRestTemplate.exchange(
                "/api/posts/" + post.getId(),
                HttpMethod.DELETE,
                request,
                PostResponseDto.class
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
