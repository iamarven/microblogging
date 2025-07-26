package com.merfonteen.postservice.service.impl;

import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.postservice.config.CacheNames;
import com.merfonteen.postservice.dto.PostCreateRequest;
import com.merfonteen.postservice.dto.PostResponse;
import com.merfonteen.postservice.dto.PostUpdateRequest;
import com.merfonteen.postservice.dto.PostsSearchRequest;
import com.merfonteen.postservice.dto.UserPostsPageResponse;
import com.merfonteen.postservice.mapper.PostMapper;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.PostPublisher;
import com.merfonteen.postservice.service.PostService;
import com.merfonteen.postservice.service.RateLimiterService;
import com.merfonteen.postservice.util.AuthUtil;
import com.merfonteen.postservice.util.PostFactory;
import com.merfonteen.postservice.util.PostValidator;
import com.merfonteen.postservice.util.StringRedisCounter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Primary
@RequiredArgsConstructor
@Service
public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;
    private final PostFactory postFactory;
    private final PostValidator postValidator;
    private final PostPublisher postPublisher;
    private final PostRepository postRepository;
    private final StringRedisCounter redisCounter;
    private final RateLimiterService rateLimiterService;

    @Cacheable(value = CacheNames.POST_BY_ID, key = "#id")
    @Override
    public PostResponse getPostById(Long id) {
        Post post = findPostByIdOrThrowException(id);
        log.info("Getting post with id '{}'", id);
        return postMapper.toDto(post);
    }

    @Override
    public Long getPostAuthorId(Long postId) {
        Post post = findPostByIdOrThrowException(postId);
        return post.getAuthorId();
    }

    @Cacheable(value = CacheNames.USER_POSTS, key = "#userId")
    @Override
    public UserPostsPageResponse getUserPosts(Long userId, PostsSearchRequest request) {
        postValidator.checkUserExists(userId);

        Pageable pageable = postMapper.buildPageable(request);
        Page<Post> userPostsPage = postRepository.findAllByAuthorId(userId, pageable);
        List<PostResponse> userPostsDto = postMapper.toListDtos(userPostsPage.getContent());

        log.info("Found {} posts for user '{}'", userPostsDto.size(), userId);

        return postMapper.buildUserPostsPageResponse(userPostsDto, userPostsPage);
    }

    @Override
    public Long getPostCount(Long userId) {
        String cachedValue = redisCounter.getCachedValue(userId);

        if (cachedValue != null) {
            return Long.parseLong(cachedValue);
        }

        Long numberOfPostsFromDb = postRepository.countByAuthorId(userId);
        redisCounter.putCounter(userId, numberOfPostsFromDb);

        return numberOfPostsFromDb;
    }

    @CacheEvict(value = CacheNames.USER_POSTS, key = "#currentUserId")
    @Transactional
    @Override
    public PostResponse createPost(Long currentUserId, PostCreateRequest request) {
        rateLimiterService.validatePostCreationLimit(currentUserId);

        Post post = postFactory.create(currentUserId, request);
        Post savedPost = postRepository.save(post);
        log.info("Post with id '{}' successfully created by user '{}'", savedPost.getId(), currentUserId);

        redisCounter.incrementCounter(currentUserId);
        postPublisher.publishPostCreatedEvent(post.getId(), currentUserId);

        return postMapper.toDto(savedPost);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.POST_BY_ID, key = "#id"),
                    @CacheEvict(value = CacheNames.USER_POSTS, key = "#currentUserId")
            })
    @Transactional
    @Override
    public PostResponse updatePost(Long id, PostUpdateRequest updateDto, Long currentUserId) {
        Post postToUpdate = findPostByIdOrThrowException(id);
        AuthUtil.requireSelfAccess(postToUpdate.getAuthorId(), currentUserId);

        Optional.ofNullable(updateDto.getContent()).ifPresent(postToUpdate::setContent);
        postToUpdate.setUpdatedAt(Instant.now());

        Post updatedPost = postRepository.save(postToUpdate);
        log.info("Post with id '{}' successfully updated by user with id: '{}'", id, currentUserId);

        return postMapper.toDto(updatedPost);
    }

    @Caching(
            evict = {
                    @CacheEvict(value = CacheNames.POST_BY_ID, key = "#id"),
                    @CacheEvict(value = CacheNames.USER_POSTS, key = "#currentUserId")
            })
    @Transactional
    @Override
    public void deletePost(Long id, Long currentUserId) {
        Post postToDelete = findPostByIdOrThrowException(id);
        AuthUtil.requireSelfAccess(postToDelete.getAuthorId(), currentUserId);

        postRepository.deleteById(postToDelete.getId());
        log.info("Post with id '{}' successfully deleted by user '{}'", id, currentUserId);

        postPublisher.publishPostRemovedEvent(id, currentUserId);
        redisCounter.decrementCounter(currentUserId);
    }

    private Post findPostByIdOrThrowException(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Post with id '%d' not found", id)));
    }
}
