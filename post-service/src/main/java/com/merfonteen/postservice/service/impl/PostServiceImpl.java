package com.merfonteen.postservice.service.impl;

import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.postservice.client.UserClient;
import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.dto.PostUpdateDto;
import com.merfonteen.postservice.dto.PostsSearchRequest;
import com.merfonteen.postservice.dto.UserPostsPageResponseDto;
import com.merfonteen.postservice.kafkaProducer.PostEventProducer;
import com.merfonteen.postservice.mapper.PostMapper;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.PostService;
import com.merfonteen.postservice.service.RateLimiterService;
import com.merfonteen.postservice.util.AuthUtil;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Primary
@RequiredArgsConstructor
@Service
public class PostServiceImpl implements PostService {
    private final UserClient userClient;
    private final PostMapper postMapper;
    private final PostRepository postRepository;
    private final PostEventProducer postEventProducer;
    private final RateLimiterService rateLimiterService;
    private final StringRedisTemplate stringRedisTemplate;

    @Cacheable(value = "post-by-id", key = "#id")
    @Override
    public PostResponseDto getPostById(Long id) {
        Post post = findPostByIdOrThrowException(id);
        log.info("Getting post with id '{}'", id);
        return postMapper.toDto(post);
    }

    @Override
    public Long getPostAuthorId(Long postId) {
        Post post = findPostByIdOrThrowException(postId);
        return post.getAuthorId();
    }

    @Cacheable(value = "user-posts", key = "#userId")
    @Override
    public UserPostsPageResponseDto getUserPosts(Long userId, PostsSearchRequest request) {
        checkUserExistsByUserClient(userId);

        Pageable pageable = postMapper.buildPageable(request);
        Page<Post> userPostsPage = postRepository.findAllByAuthorId(userId, pageable);
        List<PostResponseDto> userPostsDto = postMapper.toListDtos(userPostsPage.getContent());

        log.info("Found {} posts for user '{}'", userPostsDto.size(), userId);

        return UserPostsPageResponseDto.builder()
                .posts(userPostsDto)
                .currentPage(userPostsPage.getNumber())
                .totalPages(userPostsPage.getTotalPages())
                .totalElements(userPostsPage.getTotalElements())
                .isLastPage(userPostsPage.isLast())
                .build();
    }

    @Override
    public Long getPostCount(Long userId) {
        String cacheKey = "post:count:user:" + userId;
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);

        if (cachedValue != null) {
            return Long.parseLong(cachedValue);
        }

        Long numberOfPostsFromDb = postRepository.countByAuthorId(userId);

        stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(numberOfPostsFromDb), Duration.ofMinutes(10));

        return numberOfPostsFromDb;
    }

    @CacheEvict(value = "user-posts", key = "#currentUserId")
    @Transactional
    @Override
    public PostResponseDto createPost(Long currentUserId, PostCreateDto createDto) {
        checkUserExistsByUserClient(currentUserId);

        rateLimiterService.validatePostCreationLimit(currentUserId);

        Post post = Post.builder()
                .authorId(currentUserId)
                .content(createDto.getContent())
                .createdAt(Instant.now())
                .build();

        postRepository.save(post);
        log.info("Post with id '{}' successfully created by user '{}'", post.getId(), currentUserId);

        stringRedisTemplate.opsForValue().increment("post:count:user:" + currentUserId);
        postEventProducer.sendPostCreatedEvent(new PostCreatedEvent(post.getId(), currentUserId, Instant.now()));

        return postMapper.toDto(post);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "post-by-id", key = "#id"),
                    @CacheEvict(value = "user-posts", key = "#currentUserId")
            })
    @Transactional
    @Override
    public PostResponseDto updatePost(Long id, PostUpdateDto updateDto, Long currentUserId) {
        Post postToUpdate = findPostByIdOrThrowException(id);
        AuthUtil.requireSelfAccess(postToUpdate.getAuthorId(), currentUserId);

        Optional.ofNullable(updateDto.getContent()).ifPresent(postToUpdate::setContent);
        postToUpdate.setUpdatedAt(Instant.now());

        postRepository.save(postToUpdate);
        log.info("Post with id '{}' successfully updated by user with id: '{}'", id, currentUserId);

        return postMapper.toDto(postToUpdate);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = "post-by-id", key = "#id"),
                    @CacheEvict(value = "user-posts", key = "#currentUserId")
            })
    @Transactional
    @Override
    public PostResponseDto deletePost(Long id, Long currentUserId) {
        Post postToDelete = findPostByIdOrThrowException(id);
        AuthUtil.requireSelfAccess(postToDelete.getAuthorId(), currentUserId);

        postRepository.deleteById(postToDelete.getId());
        log.info("Post with id '{}' successfully deleted by user '{}'", id, currentUserId);

        postEventProducer.sendPostRemovedEvent(new PostRemovedEvent(id, currentUserId));

        String cacheKey = "post:count:user:" + currentUserId;
        if (stringRedisTemplate.hasKey(cacheKey)) {
            stringRedisTemplate.opsForValue().decrement(cacheKey);
        }

        return postMapper.toDto(postToDelete);
    }

    private void checkUserExistsByUserClient(Long userId) {
        try {
            userClient.checkUserExists(userId);
        } catch (FeignException.NotFound e) {
            log.error("User with id '{}' not found", userId);
            throw new NotFoundException(String.format("User with id '%d' not found", userId));
        }
    }

    private Post findPostByIdOrThrowException(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Post with id '%d' not found", id)));
    }
}
