package com.merfonteen.postservice.service.impl;

import com.merfonteen.postservice.client.UserClient;
import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.dto.PostUpdateDto;
import com.merfonteen.postservice.dto.UserPostsPageResponseDto;
import com.merfonteen.postservice.exception.NotFoundException;
import com.merfonteen.postservice.mapper.PostMapper;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.model.enums.PostSortField;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.PostCacheService;
import com.merfonteen.postservice.service.PostService;
import com.merfonteen.postservice.service.RateLimiterService;
import com.merfonteen.postservice.util.AuthUtil;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Primary
@RequiredArgsConstructor
@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostRepository postRepository;
    private final UserClient userClient;
    private final RateLimiterService rateLimiterService;
    private final PostCacheService postCacheService;

    @Cacheable(value = "post-by-id", key = "#id", unless = "#result == null")
    @Override
    public PostResponseDto getPostById(Long id) {
        Post post = findPostByIdOrThrowException(id);
        log.info("Getting post with id '{}'", id);
        return postMapper.toDto(post);
    }

    @Cacheable(value = "user-posts", key = "#userId + ':' + #page + ':' + #size")
    @Override
    public UserPostsPageResponseDto getUserPosts(Long userId, int page, int size, PostSortField sortField) {
        checkUserExistsByUserClient(userId);

        if(size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField.getFieldName()));
        Page<Post> userPostsPage = postRepository.findAllByAuthorId(userId, pageable);
        List<PostResponseDto> userPostsDto = postMapper.toListDtos(userPostsPage.getContent());

        log.info("Getting posts for user '{}', page={}, size={}, sortBy={}", userId, page, size, sortField.getFieldName());

        return UserPostsPageResponseDto.builder()
                .posts(userPostsDto)
                .currentPage(userPostsPage.getNumber())
                .totalPages(userPostsPage.getTotalPages())
                .totalElements(userPostsPage.getTotalElements())
                .isLastPage(userPostsPage.isLast())
                .build();
    }

    @Transactional
    @Override
    public PostResponseDto createPost(Long currentUserId, PostCreateDto createDto) {
        checkUserExistsByUserClient(currentUserId);

        rateLimiterService.validatePostCreationLimit(currentUserId);

        Post post = Post.builder()
                .authorId(currentUserId)
                .content(createDto.getContent())
                .mediaUrl(createDto.getMediaUrl())
                .createdAt(Instant.now())
                .build();

        postRepository.save(post);
        log.info("Post with id '{}' successfully created by user '{}'", post.getId(), currentUserId);

        postCacheService.evictUserPostsCacheByUserId(currentUserId);
        return postMapper.toDto(post);
    }

    @CacheEvict(value = "post-by-id", key = "#id")
    @Transactional
    @Override
    public PostResponseDto updatePost(Long id, PostUpdateDto updateDto, Long currentUserId) {
        Post postToUpdate = findPostByIdOrThrowException(id);
        AuthUtil.requireSelfAccess(postToUpdate.getAuthorId(), currentUserId);

        Optional.ofNullable(updateDto.getContent()).ifPresent(postToUpdate::setContent);
        Optional.ofNullable(updateDto.getMediaUrl()).ifPresent(postToUpdate::setMediaUrl);
        postToUpdate.setUpdatedAt(Instant.now());

        postRepository.save(postToUpdate);
        log.info("Post with id '{}' successfully updated by user with id: '{}'", id, currentUserId);

        postCacheService.evictUserPostsCacheByUserId(currentUserId);
        return postMapper.toDto(postToUpdate);
    }

    @CacheEvict(value = "post-by-id", key = "#id")
    @Transactional
    @Override
    public PostResponseDto deletePost(Long id, Long currentUserId) {
        Post postToDelete = findPostByIdOrThrowException(id);
        AuthUtil.requireSelfAccess(postToDelete.getAuthorId(), currentUserId);

        postRepository.deleteById(postToDelete.getId());
        log.info("Post with id '{}' successfully deleted by user '{}'", id, currentUserId);

        postCacheService.evictUserPostsCacheByUserId(currentUserId);
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
