package com.merfonteen.commentservice.service.impl;

import com.merfonteen.commentservice.client.PostClient;
import com.merfonteen.commentservice.dto.CommentPageResponseDto;
import com.merfonteen.commentservice.dto.CommentRequestDto;
import com.merfonteen.commentservice.dto.CommentResponseDto;
import com.merfonteen.commentservice.dto.CommentUpdateDto;
import com.merfonteen.commentservice.kafka.eventProducer.CommentEventProducer;
import com.merfonteen.commentservice.mapper.CommentMapper;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.model.enums.CommentSortField;
import com.merfonteen.commentservice.repository.CommentRepository;
import com.merfonteen.commentservice.util.CommentRateLimiter;
import com.merfonteen.commentservice.util.RedisCacheCleaner;
import com.merfonteen.exceptions.ForbiddenException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private PostClient postClient;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RedisCacheCleaner redisCacheCleaner;

    @Mock
    private CommentRateLimiter commentRateLimiter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private CommentEventProducer commentEventProducer;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void testGetCommentsOnPost_ShouldReturnPageWithComments() {
        Long postId = 1L;
        int page = 0;
        int size = 10;
        CommentSortField sortField = CommentSortField.CREATED_AT;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField.getFieldName()));

        Comment comment = Comment.builder()
                .id(1L)
                .postId(postId)
                .userId(2L)
                .content("content")
                .createdAt(Instant.now())
                .build();

        CommentResponseDto dto = CommentResponseDto.builder()
                .id(1L)
                .postId(postId)
                .userId(2L)
                .content("content")
                .createdAt(comment.getCreatedAt())
                .build();

        when(commentRepository.findAllByPostId(postId, pageRequest)).thenReturn(new PageImpl<>(List.of(comment)));
        when(commentMapper.toDtos(List.of(comment))).thenReturn(List.of(dto));

        CommentPageResponseDto result = commentService.getCommentsOnPost(postId, page, size, sortField);

        assertEquals(1, result.getCommentDtos().size());
        assertEquals(dto.getId(), result.getCommentDtos().getFirst().getId());
        assertEquals(page, result.getCurrentPage());
        assertEquals(1L, result.getTotalElements());
        verify(commentRepository).findAllByPostId(postId, pageRequest);
        verify(commentMapper).toDtos(List.of(comment));
    }

    @Test
    void testGetCommentsOnPost_WhenSizeGreaterThanLimit_ShouldUseMaxLimit() {
        Long postId = 1L;
        int page = 0;
        int requestedSize = 150;
        CommentSortField sortField = CommentSortField.CREATED_AT;
        PageRequest limited = PageRequest.of(page, 100, Sort.by(Sort.Direction.DESC, sortField.getFieldName()));

        when(commentRepository.findAllByPostId(postId, limited)).thenReturn(Page.empty());
        when(commentMapper.toDtos(anyList())).thenReturn(List.of());

        commentService.getCommentsOnPost(postId, page, requestedSize, sortField);

        verify(commentRepository).findAllByPostId(postId, limited);
    }

    @Test
    void testGetCommentCountForPost_WhenValueInCache_ShouldReturnCachedValue() {
        Long postId = 1L;
        String cacheKey = "comment:count:post:" + postId;
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn("5");

        Long result = commentService.getCommentCountForPost(postId);

        assertEquals(5L, result);
        verify(commentRepository, never()).countAllByPostId(postId);
    }

    @Test
    void testGetCommentCountForPost_WhenCacheMiss_ShouldQueryDatabaseAndCacheValue() {
        Long postId = 1L;
        String cacheKey = "comment:count:post:" + postId;
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(commentRepository.countAllByPostId(postId)).thenReturn(3L);

        Long result = commentService.getCommentCountForPost(postId);

        assertEquals(3L, result);
        verify(commentRepository).countAllByPostId(postId);
        verify(valueOperations).set(cacheKey, "3", Duration.ofMinutes(10));
    }

    @Test
    void testCreateComment_Success() {
        CommentRequestDto requestDto = CommentRequestDto.builder()
                .postId(1L)
                .content("text")
                .build();
        Long currentUserId = 10L;

        Comment savedComment = Comment.builder()
                .id(1L)
                .postId(requestDto.getPostId())
                .userId(currentUserId)
                .content(requestDto.getContent())
                .createdAt(Instant.now())
                .build();

        CommentResponseDto responseDto = CommentResponseDto.builder()
                .id(savedComment.getId())
                .postId(savedComment.getPostId())
                .userId(savedComment.getUserId())
                .content(savedComment.getContent())
                .build();

        doNothing().when(postClient).checkPostExists(requestDto.getPostId());
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(commentMapper.toDto(savedComment)).thenReturn(responseDto);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        CommentResponseDto result = commentService.createComment(requestDto, currentUserId);

        assertEquals(responseDto.getId(), result.getId());
        verify(commentRateLimiter).limitLeavingComments(currentUserId);
        verify(commentRepository).save(any(Comment.class));
        verify(commentEventProducer).sendCommentCreatedEvent(any(CommentCreatedEvent.class));
        verify(redisCacheCleaner).evictCommentCacheOnPostByPostId(requestDto.getPostId());
        verify(valueOperations).increment("comment:count:post:" + savedComment.getPostId());
    }

    @Test
    void testCreateComment_WhenPostNotFound_ShouldThrowException() {
        CommentRequestDto dto = CommentRequestDto.builder().postId(10L).build();
        doThrow(FeignException.NotFound.class)
                .when(postClient)
                .checkPostExists(dto.getPostId());
        assertThrows(NotFoundException.class, () -> commentService.createComment(dto, 100L));
    }

    @Test
    void testUpdateComment_Success() {
        Long commentId = 1L;
        Long currentUserId = 5L;
        CommentUpdateDto updateDto = CommentUpdateDto.builder()
                .content("updated")
                .build();

        Comment comment = Comment.builder()
                .id(commentId)
                .postId(2L)
                .userId(currentUserId)
                .content("old")
                .build();

        Comment updatedComment = Comment.builder()
                .id(commentId)
                .postId(2L)
                .userId(currentUserId)
                .content(updateDto.getContent())
                .updatedAt(Instant.now())
                .build();

        CommentResponseDto responseDto = CommentResponseDto.builder()
                .id(commentId)
                .postId(2L)
                .userId(currentUserId)
                .content(updateDto.getContent())
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(updatedComment);
        when(commentMapper.toDto(updatedComment)).thenReturn(responseDto);

        CommentResponseDto result = commentService.updateComment(commentId, updateDto, currentUserId);

        assertEquals(updateDto.getContent(), result.getContent());
        verify(redisCacheCleaner).evictCommentCacheOnPostByPostId(comment.getPostId());
    }

    @Test
    void testUpdateComment_WhenCommentNotFound_ShouldThrowException() {
        Long commentId = 1L;
        CommentUpdateDto updateDto = new CommentUpdateDto();

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.updateComment(commentId, updateDto, 10L));
    }

    @Test
    void testUpdateComment_WhenNotAuthorized_ShouldThrowException() {
        Long commentId = 10L;
        CommentUpdateDto updateDto = new CommentUpdateDto();
        Long currentUserId = 1L;

        Comment commentToUpdate = Comment.builder()
                .id(commentId)
                .userId(100L)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(commentToUpdate));

        Exception exception = assertThrows(ForbiddenException.class,
                () -> commentService.updateComment(commentId, updateDto, currentUserId));

        assertEquals("You cannot update not your own comment", exception.getMessage());
    }

    @Test
    void testDeleteComment_Success() {
        Long commentId = 1L;
        Long currentUserId = 10L;

        Comment comment = Comment.builder()
                .id(commentId)
                .postId(2L)
                .userId(currentUserId)
                .content("content")
                .build();

        CommentResponseDto responseDto = CommentResponseDto.builder()
                .id(commentId)
                .postId(2L)
                .userId(currentUserId)
                .content("content")
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentMapper.toDto(comment)).thenReturn(responseDto);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        CommentResponseDto result = commentService.deleteComment(commentId, currentUserId);

        assertEquals(commentId, result.getId());
        verify(commentRepository).delete(comment);
        verify(commentEventProducer).sendCommentRemovedEvent(any(CommentRemovedEvent.class));
        verify(redisCacheCleaner).evictCommentCacheOnPostByPostId(comment.getPostId());
        verify(valueOperations).decrement("comment:count:post:" + comment.getPostId());
    }

    @Test
    void testDeleteComment_WhenCommentNotFound_ShouldThrowException() {
        Long commentId = 1L;
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> commentService.deleteComment(commentId, 10L));
    }

    @Test
    void testDeleteComment_WhenNotAuthorized_ShouldThrowException() {
        Long commentId = 10L;
        Long currentUserId = 1L;

        Comment commentToDelete = Comment.builder()
                .id(commentId)
                .userId(100L)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(commentToDelete));

        Exception exception = assertThrows(ForbiddenException.class,
                () -> commentService.deleteComment(commentId, currentUserId));

        assertEquals("You cannot update not your own comment", exception.getMessage());
    }
}