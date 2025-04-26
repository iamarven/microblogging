package com.merfonteen.postservice.service.impl;

import com.merfonteen.postservice.client.UserClient;
import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.dto.UserPostsPageResponseDto;
import com.merfonteen.postservice.entity.Post;
import com.merfonteen.postservice.exception.NotFoundException;
import com.merfonteen.postservice.mapper.PostMapper;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.PostService;
import com.merfonteen.postservice.service.RateLimiterService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Primary
@RequiredArgsConstructor
@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostRepository postRepository;
    private final UserClient userClient;
    private final RateLimiterService rateLimiterService;

    @Override
    public PostResponseDto getPostById(Long id) {
        Post post = findPostByIdOrThrowException(id);
        log.info("Getting post with id '{}'", id);
        return postMapper.toDto(post);
    }

    @Override
    public UserPostsPageResponseDto getUserPosts(Long userId, int page, int size) {
        checkUserExistsByUserClient(userId);

        if(size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> userPostsPage = postRepository.findAllByAuthorId(userId, pageable);
        List<PostResponseDto> userPostsDto = postMapper.toListDtos(userPostsPage.getContent());

        log.info("Getting posts for user with id: '{}' with pagination: page={}, size={}", userId, page, size);

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

        return postMapper.toDto(post);
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
