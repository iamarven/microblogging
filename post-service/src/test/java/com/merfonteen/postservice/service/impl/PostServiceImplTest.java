package com.merfonteen.postservice.service.impl;

import com.merfonteen.exceptions.ForbiddenException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.exceptions.TooManyRequestsException;
import com.merfonteen.postservice.client.UserClient;
import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.dto.PostUpdateDto;
import com.merfonteen.postservice.dto.PostsSearchRequest;
import com.merfonteen.postservice.dto.UserPostsPageResponseDto;
import com.merfonteen.postservice.kafkaProducer.PostEventProducer;
import com.merfonteen.postservice.mapper.PostMapper;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.model.enums.PostSortField;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.RateLimiterService;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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
    private UserClient userClient;

    @Mock
    private PostMapper postMapper;

    @Mock
    private PostRepository postRepository;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private PostEventProducer postEventProducer;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private PostServiceImpl postService;

    @Test
    void testGetPostById_Success() {
        Post post = buildPost();
        PostResponseDto expected = buildPostResponseDto(CONTENT);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.ofNullable(post));
        when(postMapper.toDto(post)).thenReturn(expected);

        PostResponseDto result = postService.getPostById(POST_ID);

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
        PostResponseDto postDto = buildPostResponseDto(Instant.now().minusSeconds(3600000));
        Page<Post> userPostsPage = buildPostsPage();
        List<PostResponseDto> postResponseDtos = new ArrayList<>(List.of(postDto));

        when(postRepository.findAllByAuthorId(AUTHOR_ID, pageRequest)).thenReturn(userPostsPage);
        when(postMapper.toListDtos(userPostsPage.getContent())).thenReturn(postResponseDtos);
        when(postMapper.buildPageable(request)).thenReturn(pageRequest);

        UserPostsPageResponseDto result = postService.getUserPosts(AUTHOR_ID, request);

        assertThat(result.getPosts()).isEqualTo(postResponseDtos);
        verify(userClient).checkUserExists(AUTHOR_ID);
    }

    @Test
    void testGetUserPosts_WhenUserNotFound_ShouldThrowException() {
        doThrow(FeignException.NotFound.class)
                .when(userClient)
                .checkUserExists(AUTHOR_ID);

        Exception exception = assertThrows(NotFoundException.class, () ->
                postService.getUserPosts(AUTHOR_ID, buildPostsSearchRequest()));

        assertEquals(USER_NOT_FOUND_EX, exception.getMessage());
    }

    @Test
    void testCreatePost_Success() {
        PostCreateDto postCreateDto = buildPostCreateDto();
        Post post = buildPost();
        PostResponseDto expected = buildPostResponseDto(CONTENT);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(postMapper.toDto(any(Post.class))).thenReturn(expected);

        PostResponseDto result = postService.createPost(AUTHOR_ID, postCreateDto);

        assertThat(result).isEqualTo(expected);

        verify(userClient).checkUserExists(AUTHOR_ID);
        verify(rateLimiterService).validatePostCreationLimit(AUTHOR_ID);
    }

    @Test
    void testCreatePost_WhenRateLimitExceeded_ShouldThrowException() {
        doThrow(new TooManyRequestsException(LIMIT_EX))
                .when(rateLimiterService)
                .validatePostCreationLimit(AUTHOR_ID);

        assertThrows(TooManyRequestsException.class, () -> postService.createPost(AUTHOR_ID, buildPostCreateDto()));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_WhenUserNotFound_ShouldThrowException() {
        doThrow(FeignException.NotFound.class)
                .when(userClient)
                .checkUserExists(AUTHOR_ID);

        Exception exception = assertThrows(NotFoundException.class,
                () -> postService.createPost(AUTHOR_ID, buildPostCreateDto()));

        assertEquals(USER_NOT_FOUND_EX, exception.getMessage());
    }

    @Test
    void testUpdatePost_Success() {
        PostUpdateDto postUpdateDto = buildPostUpdateDto();
        Post postToUpdate = buildPost();
        Post savedPost = buildUpdatedPost();
        PostResponseDto postDto = buildPostResponseDto(UPDATED_CONTENT);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.ofNullable(postToUpdate));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postMapper.toDto(any(Post.class))).thenReturn(postDto);

        PostResponseDto result = postService.updatePost(POST_ID, postUpdateDto, AUTHOR_ID);

        assertThat(result).isEqualTo(postDto);
    }

    @Test
    void testUpdatePost_WhenPostNotFound_ShouldThrowException() {
        PostUpdateDto postUpdateDto = buildPostUpdateDto();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> postService.updatePost(POST_ID, postUpdateDto, AUTHOR_ID));

        assertEquals(POST_NOT_FOUND_EX, exception.getMessage());
    }

    @Test
    void testUpdatePost_WhenNotAllowed_ShouldThrowException() {
        PostUpdateDto postUpdateDto = buildPostUpdateDto();
        Post postToUpdate = buildPostWithUnknownUser();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.ofNullable(postToUpdate));

        Exception exception = assertThrows(ForbiddenException.class,
                () -> postService.updatePost(POST_ID, postUpdateDto, AUTHOR_ID));

        assertEquals(NOT_ALLOWED_EX, exception.getMessage());
    }

    @Test
    void testDeletePost_Success() {
        Post postToDelete = buildPost();
        PostResponseDto postDto = buildPostResponseDto(CONTENT);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.ofNullable(postToDelete));
        when(postMapper.toDto(any(Post.class))).thenReturn(postDto);

        PostResponseDto result = postService.deletePost(POST_ID, AUTHOR_ID);

        assertThat(result).isEqualTo(postDto);
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
        static final String USER_NOT_FOUND_EX = "User with id '%d' not found".formatted(AUTHOR_ID);

        static PageRequest buildPageRequest() {
            return PageRequest.of(PAGE, SIZE, Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD.getFieldName()));
        }

        static PostCreateDto buildPostCreateDto() {
            return PostCreateDto.builder()
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

        static PostUpdateDto buildPostUpdateDto() {
            return PostUpdateDto.builder()
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

        static PostResponseDto buildPostResponseDto(String content) {
            return PostResponseDto.builder()
                    .id(POST_ID)
                    .authorId(1L)
                    .content(content)
                    .createdAt(Instant.now())
                    .build();
        }

        static PostResponseDto buildPostResponseDto(Instant createdAt) {
            return PostResponseDto.builder()
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