package com.merfonteen.postservice.service.impl;

import com.merfonteen.exceptions.ForbiddenException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.exceptions.TooManyRequestsException;
import com.merfonteen.postservice.client.UserClient;
import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.dto.PostUpdateDto;
import com.merfonteen.postservice.dto.UserPostsPageResponseDto;
import com.merfonteen.postservice.kafkaProducer.PostEventProducer;
import com.merfonteen.postservice.mapper.PostMapper;
import com.merfonteen.postservice.model.Post;
import com.merfonteen.postservice.model.enums.PostSortField;
import com.merfonteen.postservice.repository.PostRepository;
import com.merfonteen.postservice.service.PostCacheService;
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
    private PostCacheService postCacheService;

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
        Long id = 1L;

        Post post = Post.builder()
                .id(1L)
                .content("content")
                .build();

        PostResponseDto expected = PostResponseDto.builder()
                .id(1L)
                .content("content")
                .build();

        when(postRepository.findById(id)).thenReturn(Optional.ofNullable(post));
        when(postMapper.toDto(post)).thenReturn(expected);

        PostResponseDto result = postService.getPostById(id);

        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getContent(), result.getContent());
    }

    @Test
    void testGetPostById_WhenPostNotFound_ShouldThrowException() {
        Long id = 1L;
        when(postRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> postService.getPostById(id));
    }

    @Test
    void testGetUserPosts_Success() {
        Long userId = 1L;
        int page = 0;
        int size = 10;
        PostSortField sortField = PostSortField.CREATED_AT;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField.getFieldName()));

        Post post = Post.builder()
                .id(1L)
                .authorId(userId)
                .content("content")
                .createdAt(Instant.now().minusSeconds(3600000))
                .build();

        PostResponseDto postDto = PostResponseDto.builder()
                .id(1L)
                .authorId(userId)
                .content("content")
                .createdAt(Instant.now().minusSeconds(3600000))
                .build();

        List<Post> userPosts = new ArrayList<>(List.of(post));
        Page<Post> userPostsPage = new PageImpl<>(userPosts);
        List<PostResponseDto> postResponseDtos = new ArrayList<>(List.of(postDto));

        when(postRepository.findAllByAuthorId(userId, pageRequest)).thenReturn(userPostsPage);
        when(postMapper.toListDtos(userPostsPage.getContent())).thenReturn(postResponseDtos);

        UserPostsPageResponseDto result = postService.getUserPosts(userId, page, size, sortField);

        assertEquals(postDto.getId(), result.getPosts().get(0).getId());
        assertEquals(postDto.getContent(), result.getPosts().get(0).getContent());

        verify(userClient).checkUserExists(userId);
    }

    @Test
    void testGetUserPosts_WhenUserNotFound_ShouldThrowException() {
        Long userId = 1L;
        int page = 0;
        int size = 10;
        PostSortField sortField = PostSortField.CREATED_AT;

        doThrow(FeignException.NotFound.class)
                .when(userClient)
                .checkUserExists(userId);

        Exception exception = assertThrows(NotFoundException.class, () ->
                postService.getUserPosts(userId, page, size, sortField));

        assertEquals("User with id '1' not found", exception.getMessage());
    }

    @Test
    void testCreatePost_Success() {
        Long currentUserId = 1L;
        PostCreateDto postCreateDto = PostCreateDto.builder()
                .content("content")
                .build();

        Post post = Post.builder()
                .id(1L)
                .authorId(currentUserId)
                .content(postCreateDto.getContent())
                .createdAt(Instant.now())
                .build();

        PostResponseDto postDto = PostResponseDto.builder()
                .id(1L)
                .authorId(currentUserId)
                .content(postCreateDto.getContent())
                .createdAt(Instant.now())
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(postMapper.toDto(any(Post.class))).thenReturn(postDto);

        PostResponseDto result = postService.createPost(currentUserId, postCreateDto);

        assertEquals(post.getId(), result.getId());
        assertEquals(post.getContent(), result.getContent());

        verify(userClient).checkUserExists(currentUserId);
        verify(rateLimiterService).validatePostCreationLimit(currentUserId);
        verify(postCacheService).evictUserPostsCacheByUserId(currentUserId);
    }

    @Test
    void testCreatePost_WhenRateLimitExceeded_ShouldThrowException() {
        Long currentUserId = 1L;
        PostCreateDto postCreateDto = PostCreateDto.builder()
                .content("content")
                .build();

        doThrow(new TooManyRequestsException("You have exceed the allowed number of posts per minute"))
                .when(rateLimiterService)
                .validatePostCreationLimit(currentUserId);

        Exception exception = assertThrows(TooManyRequestsException.class,
                () -> postService.createPost(currentUserId, postCreateDto));

        assertEquals("You have exceed the allowed number of posts per minute", exception.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void testCreatePost_WhenUserNotFound_ShouldThrowException() {
        Long currentUserId = 1L;
        PostCreateDto postCreateDto = PostCreateDto.builder()
                .content("content")
                .build();

        doThrow(FeignException.NotFound.class)
                .when(userClient)
                .checkUserExists(currentUserId);

        Exception exception = assertThrows(NotFoundException.class,
                () -> postService.createPost(currentUserId, postCreateDto));

        assertEquals("User with id '1' not found", exception.getMessage());
    }

    @Test
    void testUpdatePost_Success() {
        Long id = 1L;
        Long currentUserId = 10L;
        PostUpdateDto postUpdateDto = PostUpdateDto.builder()
                .content("new content")
                .build();

        Post postToUpdate = Post.builder()
                .id(id)
                .authorId(currentUserId)
                .content("old content")
                .build();

        Post savedPost = Post.builder()
                .id(id)
                .authorId(currentUserId)
                .content("new content")
                .build();

        PostResponseDto postDto = PostResponseDto.builder()
                .id(1L)
                .authorId(currentUserId)
                .content(postUpdateDto.getContent())
                .build();

        when(postRepository.findById(id)).thenReturn(Optional.ofNullable(postToUpdate));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postMapper.toDto(any(Post.class))).thenReturn(postDto);

        PostResponseDto result = postService.updatePost(id, postUpdateDto, currentUserId);

        assertEquals(postDto.getId(), result.getId());
        assertEquals(postDto.getContent(), result.getContent());

        verify(postCacheService).evictUserPostsCacheByUserId(currentUserId);
    }

    @Test
    void testUpdatePost_WhenPostNotFound_ShouldThrowException() {
        Long id = 1L;
        Long currentUserId = 10L;
        PostUpdateDto postUpdateDto = PostUpdateDto.builder()
                .content("new content")
                .build();

        when(postRepository.findById(id)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> postService.updatePost(id, postUpdateDto, currentUserId));

        assertEquals("Post with id '1' not found", exception.getMessage());
    }

    @Test
    void testUpdatePost_WhenNotAllowed_ShouldThrowException() {
        Long id = 1L;
        Long currentUserId = 10L;
        PostUpdateDto postUpdateDto = PostUpdateDto.builder()
                .content("new content")
                .build();

        Post postToUpdate = Post.builder()
                .id(1L)
                .authorId(100L)
                .content("old content")
                .build();

        when(postRepository.findById(id)).thenReturn(Optional.ofNullable(postToUpdate));

        Exception exception = assertThrows(ForbiddenException.class,
                () -> postService.updatePost(id, postUpdateDto, currentUserId));

        assertEquals("You are not allowed to modify this post", exception.getMessage());
    }

    @Test
    void testDeletePost_Success() {
        Long id = 1L;
        Long currentUserId = 10L;

        Post postToDelete = Post.builder()
                .id(id)
                .authorId(currentUserId)
                .content("content")
                .build();

        PostResponseDto postDto = PostResponseDto.builder()
                .id(1L)
                .authorId(currentUserId)
                .content("content")
                .build();

        when(postRepository.findById(id)).thenReturn(Optional.ofNullable(postToDelete));
        when(postMapper.toDto(any(Post.class))).thenReturn(postDto);

        PostResponseDto result = postService.deletePost(id, currentUserId);

        assertEquals(postDto.getId(), result.getId());
        assertEquals(postDto.getContent(), result.getContent());

        verify(postCacheService).evictUserPostsCacheByUserId(currentUserId);
    }

    @Test
    void testDeletePost_WhenPostNotFound_ShouldThrowException() {
        Long id = 1L;
        Long currentUserId = 10L;

        when(postRepository.findById(id)).thenReturn(Optional.empty());

        Exception exception = assertThrows(NotFoundException.class,
                () -> postService.deletePost(id, currentUserId));

        assertEquals("Post with id '1' not found", exception.getMessage());
    }

    @Test
    void testDeletePost_WhenNotAllowed_ShouldThrowException() {
        Long id = 1L;
        Long currentUserId = 10L;

        Post postToDelete = Post.builder()
                .id(id)
                .authorId(100L)
                .content("content")
                .build();

        when(postRepository.findById(id)).thenReturn(Optional.ofNullable(postToDelete));

        Exception exception = assertThrows(ForbiddenException.class,
                () -> postService.deletePost(id, currentUserId));

        assertEquals("You are not allowed to modify this post", exception.getMessage());
    }
}