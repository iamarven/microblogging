package com.merfonteen.postservice.service.impl;

import com.merfonteen.exceptions.ForbiddenException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.exceptions.TooManyRequestsException;
import com.merfonteen.postservice.dto.PostCreateRequest;
import com.merfonteen.postservice.dto.PostResponse;
import com.merfonteen.postservice.dto.PostUpdateRequest;
import com.merfonteen.postservice.dto.PostsSearchRequest;
import com.merfonteen.postservice.dto.UserPostsPageResponse;
import com.merfonteen.postservice.mapper.PostMapper;
import com.merfonteen.postservice.model.OutboxEvent;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.model.enums.OutboxEventType;
import com.merfonteen.postservice.model.enums.PostSortField;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.OutboxService;
import com.merfonteen.postservice.service.redis.PostRateLimiter;
import com.merfonteen.postservice.service.redis.RedisCacheInvalidator;
import com.merfonteen.postservice.util.PostValidator;
import com.merfonteen.postservice.service.redis.RedisCounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.merfonteen.postservice.service.impl.PostServiceImplTest.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostMapper postMapper;

    @Mock
    private PostRepository postRepository;

    @Mock
    private RedisCounter redisCounter;

    @Mock
    private PostRateLimiter postRateLimiter;

    @Mock
    private PostValidator postValidator;

    @Mock
    private OutboxService outboxService;

    @Mock
    private RedisCacheInvalidator redisCacheInvalidator;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    void testGetPostById_Success() {
        Post post = buildPost();
        PostResponse expected = buildPostResponseDto(CONTENT);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.ofNullable(post));
        when(postMapper.toDto(post)).thenReturn(expected);

        PostResponse result = postService.getPostById(POST_ID);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testGetPostById_WhenPostNotFound_ShouldThrowException() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> postService.getPostById(POST_ID));
    }

    @Test
    void testGetUserPosts_Success() {
        PageRequest pageRequest = buildPageRequest();
        PostsSearchRequest request = buildPostsSearchRequest();
        PostResponse postDto = buildPostResponseDto(Instant.now().minusSeconds(3600000));
        Page<Post> userPostsPage = buildPostsPage();
        List<PostResponse> postResponses = new ArrayList<>(List.of(postDto));

        when(postRepository.findAllByAuthorId(AUTHOR_ID, pageRequest)).thenReturn(userPostsPage);
        when(postMapper.toListDtos(userPostsPage.getContent())).thenReturn(postResponses);
        when(postMapper.buildPageable(request)).thenReturn(pageRequest);
        when(postMapper.buildUserPostsPageResponse(postResponses, userPostsPage)).
                thenReturn(buildUserPostsPageResponse(postResponses, userPostsPage));

        UserPostsPageResponse result = postService.getUserPosts(AUTHOR_ID, request);

        assertThat(result.getPosts()).isEqualTo(postResponses);
    }

    @Test
    void testCreatePost_Success() {
        PostCreateRequest postCreateRequest = buildPostCreateDto();
        Post post = buildPost();
        PostResponse expected = buildPostResponseDto(CONTENT);

        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(postMapper.toDto(any(Post.class))).thenReturn(expected);
        when(outboxService.create(post, OutboxEventType.POST_CREATED))
                .thenReturn(buildExpectedOutboxEvent(post, OutboxEventType.POST_CREATED));

        PostResponse result = postService.createPost(AUTHOR_ID, postCreateRequest);

        assertThat(result).isEqualTo(expected);

        verify(postRateLimiter).limitPostCreation(AUTHOR_ID);
    }

    @Test
    void testCreatePost_WhenRateLimitExceeded_ShouldThrowException() {
        doThrow(new TooManyRequestsException(LIMIT_EX))
                .when(postRateLimiter)
                .limitPostCreation(AUTHOR_ID);

        assertThrows(TooManyRequestsException.class, () -> postService.createPost(AUTHOR_ID, buildPostCreateDto()));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testUpdatePost_Success() {
        PostUpdateRequest postUpdateRequest = buildPostUpdateDto();
        Post postToUpdate = buildPost();
        Post savedPost = buildUpdatedPost();
        PostResponse postDto = buildPostResponseDto(UPDATED_CONTENT);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.ofNullable(postToUpdate));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postMapper.toDto(any(Post.class))).thenReturn(postDto);

        PostResponse result = postService.updatePost(POST_ID, postUpdateRequest, AUTHOR_ID);

        assertThat(result).isEqualTo(postDto);
    }

    @Test
    void testUpdatePost_WhenPostNotFound_ShouldThrowException() {
        PostUpdateRequest postUpdateRequest = buildPostUpdateDto();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> postService.updatePost(POST_ID, postUpdateRequest, AUTHOR_ID));

        assertEquals(POST_NOT_FOUND_EX, exception.getMessage());
    }

    @Test
    void testUpdatePost_WhenNotAllowed_ShouldThrowException() {
        PostUpdateRequest postUpdateRequest = buildPostUpdateDto();
        Post postToUpdate = buildPostWithUnknownUser();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.ofNullable(postToUpdate));

        Exception exception = assertThrows(ForbiddenException.class,
                () -> postService.updatePost(POST_ID, postUpdateRequest, AUTHOR_ID));

        assertEquals(NOT_ALLOWED_EX, exception.getMessage());
    }

    @Test
    void testDeletePost_Success() {
        Post postToDelete = buildPost();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.ofNullable(postToDelete));
        when(outboxService.create(postToDelete, OutboxEventType.POST_REMOVED))
                .thenReturn(buildExpectedOutboxEvent(postToDelete, OutboxEventType.POST_REMOVED));

        postService.deletePost(POST_ID, AUTHOR_ID);
        verify(postRepository).deleteById(POST_ID);
    }

    @Test
    void testDeletePost_WhenPostNotFound_ShouldThrowException() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> postService.deletePost(POST_ID, AUTHOR_ID));

        assertEquals(POST_NOT_FOUND_EX, exception.getMessage());
    }

    @Test
    void testDeletePost_WhenNotAllowed_ShouldThrowException() {
        Post postToDelete = buildPostWithUnknownUser();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.ofNullable(postToDelete));

        Exception exception = assertThrows(ForbiddenException.class,
                () -> postService.deletePost(POST_ID, AUTHOR_ID));

        assertEquals(NOT_ALLOWED_EX, exception.getMessage());
    }

    static class TestResources {
        static final Long POST_ID = 1L;
        static final Long AUTHOR_ID = 1L;
        static final Long UNKNOWN_USER_ID = 999L;
        static final int PAGE = 0;
        static final int SIZE = 10;
        static final PostSortField DEFAULT_SORT_FIELD = PostSortField.CREATED_AT;
        static final String CONTENT = "Test content";
        static final String UPDATED_CONTENT = "New content";
        static final String LIMIT_EX = "You have exceeded the allowed number of posts per minute";
        static final String NOT_ALLOWED_EX = "You are not allowed to modify this post";
        static final String POST_NOT_FOUND_EX = "Post with id '%d' not found".formatted(POST_ID);

        static PageRequest buildPageRequest() {
            return PageRequest.of(PAGE, SIZE, Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD.getFieldName()));
        }

        static OutboxEvent buildExpectedOutboxEvent(Post post, OutboxEventType eventType) {
            return OutboxEvent.builder()
                    .aggregateType("Post")
                    .aggregateId(post.getId())
                    .eventType(eventType)
                    .sent(false)
                    .payload(null)
                    .createdAt(Instant.now())
                    .build();
        }

        static UserPostsPageResponse buildUserPostsPageResponse(List<PostResponse> posts, Page<Post> page) {
            return UserPostsPageResponse.builder()
                    .posts(posts)
                    .currentPage(page.getNumber())
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .isLastPage(page.isLast())
                    .build();
        }

        static PostCreateRequest buildPostCreateDto() {
            return PostCreateRequest.builder()
                    .content(CONTENT)
                    .build();
        }

        static PostsSearchRequest buildPostsSearchRequest() {
            return PostsSearchRequest.builder()
                    .page(PAGE)
                    .size(SIZE)
                    .sortBy(DEFAULT_SORT_FIELD.getFieldName())
                    .build();
        }

        static PostUpdateRequest buildPostUpdateDto() {
            return PostUpdateRequest.builder()
                    .content(UPDATED_CONTENT)
                    .build();
        }

        static Post buildPost() {
            return Post.builder()
                    .id(POST_ID)
                    .authorId(AUTHOR_ID)
                    .content(CONTENT)
                    .createdAt(Instant.now())
                    .build();
        }

        static Post buildUpdatedPost() {
            return Post.builder()
                    .id(POST_ID)
                    .authorId(AUTHOR_ID)
                    .content(UPDATED_CONTENT)
                    .createdAt(Instant.now())
                    .build();
        }

        static Post buildPostWithUnknownUser() {
            return Post.builder()
                    .id(POST_ID)
                    .authorId(UNKNOWN_USER_ID)
                    .content(UPDATED_CONTENT)
                    .createdAt(Instant.now())
                    .build();
        }

        static PostResponse buildPostResponseDto(String content) {
            return PostResponse.builder()
                    .id(POST_ID)
                    .authorId(1L)
                    .content(content)
                    .createdAt(Instant.now())
                    .build();
        }

        static PostResponse buildPostResponseDto(Instant createdAt) {
            return PostResponse.builder()
                    .id(POST_ID)
                    .authorId(1L)
                    .content(CONTENT)
                    .createdAt(createdAt)
                    .build();
        }

        static Page<Post> buildPostsPage() {
            return new PageImpl<>(List.of(buildPost()));
        }
    }
}