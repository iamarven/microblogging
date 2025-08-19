package com.merfonteen.commentservice.service.impl;

import com.merfonteen.commentservice.client.PostClient;
import com.merfonteen.commentservice.dto.CommentCreateRequest;
import com.merfonteen.commentservice.dto.CommentPageResponse;
import com.merfonteen.commentservice.dto.CommentResponse;
import com.merfonteen.commentservice.dto.CommentUpdateRequest;
import com.merfonteen.commentservice.dto.CommentsOnPostSearchRequest;
import com.merfonteen.commentservice.kafka.eventProducer.CommentEventProducer;
import com.merfonteen.commentservice.mapper.CommentMapper;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.model.enums.CommentSortField;
import com.merfonteen.commentservice.repository.CommentRepository;
import com.merfonteen.commentservice.service.OutboxService;
import com.merfonteen.commentservice.service.redis.CommentRateLimiter;
import com.merfonteen.commentservice.service.redis.RedisCacheInvalidator;
import com.merfonteen.commentservice.service.redis.RedisCounter;
import com.merfonteen.exceptions.ForbiddenException;
import com.merfonteen.exceptions.NotFoundException;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.merfonteen.commentservice.service.impl.CommentServiceImplTest.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private PostClient postClient;

    @Mock
    private RedisCounter redisCounter;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private OutboxService outboxService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentRateLimiter commentRateLimiter;

    @Mock
    private RedisCacheInvalidator redisCacheInvalidator;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void testGetCommentsOnPost_ShouldReturnPageWithComments() {
        CommentsOnPostSearchRequest searchRequest = buildCommentsOnPostSearchRequest();
        List<CommentResponse> dtos = List.of(buildCommentResponse(buildComment()));
        PageRequest pageRequest = buildPageRequest(searchRequest);
        Page<Comment> commentsPage = new PageImpl<>(List.of(buildComment()));
        CommentPageResponse expected = buildCommentPageResponse(commentsPage, dtos);

        when(commentRepository.findAllByPostId(POST_ID, pageRequest)).thenReturn(commentsPage);
        when(commentMapper.toDtos(List.of(buildComment()))).thenReturn(dtos);
        when(commentMapper.buildPageRequest(searchRequest)).thenReturn(pageRequest);
        when(commentMapper.buildCommentPageResponse(commentsPage, dtos)).thenReturn(expected);

        CommentPageResponse result = commentService.getCommentsOnPost(searchRequest);

        assertThat(result).isEqualTo(expected);
        verify(commentRepository).findAllByPostId(POST_ID, pageRequest);
    }

    @Test
    void testGetCommentCountForPost_WhenValueInCache_ShouldReturnCachedValue() {
        String cacheKey = buildCommentCountCacheKey(POST_ID);

        when(redisCounter.getCommentsCacheKey(POST_ID)).thenReturn(cacheKey);
        when(redisCounter.getCachedValue(cacheKey)).thenReturn("5");

        Long result = commentService.getCommentCountForPost(POST_ID);

        assertEquals(5L, result);
        verifyNoInteractions(commentRepository);
    }

    @Test
    void testGetCommentCountForPost_WhenCacheMiss_ShouldQueryDatabaseAndCacheValue() {
        String cacheKey = buildCommentCountCacheKey(POST_ID);

        when(redisCounter.getCommentsCacheKey(POST_ID)).thenReturn(cacheKey);
        when(redisCounter.getCachedValue(cacheKey)).thenReturn(null);
        when(commentRepository.countAllByPostId(POST_ID)).thenReturn(5L);

        Long result = commentService.getCommentCountForPost(POST_ID);

        assertEquals(5L, result);
        verify(commentRepository).countAllByPostId(POST_ID);
        verify(redisCounter).setCounter(cacheKey, 5L);
    }

    @Test
    void testCreateComment_Success() {
        CommentCreateRequest requestDto = buildCommentCreateRequest();
        Comment savedComment = buildComment();
        CommentResponse responseDto = buildCommentResponse(savedComment);

        doNothing().when(postClient).checkPostExists(requestDto.getPostId());
        when(redisCounter.getCommentsCacheKey(POST_ID)).thenReturn(buildCommentCountCacheKey(POST_ID));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(commentMapper.toDto(savedComment)).thenReturn(responseDto);

        CommentResponse result = commentService.createComment(requestDto, USER_ID);

        assertEquals(responseDto.getId(), result.getId());
        verify(commentRateLimiter).limitLeavingComments(USER_ID);
        verify(redisCounter).incrementCounter(buildCommentCountCacheKey(POST_ID));
        verify(redisCacheInvalidator).evictPostsCache(POST_ID);
    }

    @Test
    void testCreateComment_WhenPostNotFound_ShouldThrowException() {
        CommentCreateRequest dto = buildCommentCreateRequest();
        doThrow(FeignException.NotFound.class)
                .when(postClient)
                .checkPostExists(dto.getPostId());
        assertThrows(NotFoundException.class, () -> commentService.createComment(dto, USER_ID));
    }

    @Test
    void testUpdateComment_Success() {
        CommentUpdateRequest updateRequest = buildCommentUpdateRequest();
        Comment comment = buildComment();
        Comment updatedComment = buildUpdatedContent();
        CommentResponse responseDto = buildCommentResponse(updatedComment);

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(updatedComment);
        when(commentMapper.toDto(updatedComment)).thenReturn(responseDto);

        CommentResponse result = commentService.updateComment(COMMENT_ID, updateRequest, USER_ID);

        assertThat(result).isEqualTo(responseDto);
        verify(redisCacheInvalidator).evictPostsCache(POST_ID);
    }

    @Test
    void testUpdateComment_WhenCommentNotFound_ShouldThrowException() {
        CommentUpdateRequest updateDto = new CommentUpdateRequest();

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> commentService.updateComment(COMMENT_ID, updateDto, USER_ID));
    }

    @Test
    void testUpdateComment_WhenNotAuthorized_ShouldThrowException() {
        CommentUpdateRequest updateDto = new CommentUpdateRequest();
        Comment commentToUpdate = Comment.builder().userId(ANOTHER_USER_ID).build();

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(commentToUpdate));

        Exception exception = assertThrows(ForbiddenException.class,
                () -> commentService.updateComment(COMMENT_ID, updateDto, USER_ID));

        assertEquals("You cannot update not your own comment", exception.getMessage());
    }

    @Test
    void testDeleteComment_Success() {
        Comment comment = buildComment();

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(redisCounter.getCommentsCacheKey(COMMENT_ID)).thenReturn(buildCommentCountCacheKey(COMMENT_ID));

        commentService.deleteComment(COMMENT_ID, USER_ID);

        verify(commentRepository).delete(comment);
        verify(redisCounter).decrementCounter(buildCommentCountCacheKey(POST_ID));
        verify(redisCacheInvalidator).evictPostsCache(POST_ID);
    }

    @Test
    void testDeleteComment_WhenCommentNotFound_ShouldThrowException() {
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> commentService.deleteComment(COMMENT_ID, USER_ID));
    }

    @Test
    void testDeleteComment_WhenNotAuthorized_ShouldThrowException() {
        Comment commentToDelete = buildComment();

        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(commentToDelete));

        Exception exception = assertThrows(ForbiddenException.class,
                () -> commentService.deleteComment(COMMENT_ID, ANOTHER_USER_ID));

        assertEquals("You cannot update not your own comment", exception.getMessage());
    }

    static class TestResources {
        static final Long POST_ID = 1L;
        static final Long COMMENT_ID = 1L;
        static final Long USER_ID = 1L;
        static final Long ANOTHER_USER_ID = 100L;
        static final int PAGE = 0;
        static final int SIZE = 10;
        static final String COMMENT_CONTENT = "Some content";
        static final String NEW_CONTENT = "New content";
        static final String COMMENT_COUNT_CACHE_KEY = "comment:count:post:";
        static final Instant CREATED_AT = Instant.parse("2025-10-05T10:00:00.00Z");

        static String buildCommentCountCacheKey(Long postId) {
            return COMMENT_COUNT_CACHE_KEY + postId;
        }

        static CommentCreateRequest buildCommentCreateRequest() {
            return CommentCreateRequest.builder()
                    .postId(POST_ID)
                    .content(COMMENT_CONTENT)
                    .build();
        }

        static CommentUpdateRequest buildCommentUpdateRequest() {
            return CommentUpdateRequest.builder()
                    .content(NEW_CONTENT)
                    .build();
        }

        static CommentsOnPostSearchRequest buildCommentsOnPostSearchRequest() {
            return CommentsOnPostSearchRequest.builder()
                    .postId(POST_ID)
                    .page(PAGE)
                    .size(SIZE)
                    .sortBy("createdAt")
                    .build();
        }

        static Comment buildComment() {
            return Comment.builder()
                    .id(COMMENT_ID)
                    .postId(POST_ID)
                    .userId(USER_ID)
                    .content(COMMENT_CONTENT)
                    .createdAt(CREATED_AT)
                    .build();
        }

        static Comment buildUpdatedContent() {
            return Comment.builder()
                    .id(COMMENT_ID)
                    .postId(POST_ID)
                    .userId(USER_ID)
                    .content(NEW_CONTENT)
                    .createdAt(CREATED_AT)
                    .build();
        }

        static CommentResponse buildCommentResponse(Comment comment) {
            return CommentResponse.builder()
                    .id(comment.getId())
                    .postId(comment.getPostId())
                    .userId(comment.getUserId())
                    .content(comment.getContent())
                    .createdAt(comment.getCreatedAt())
                    .build();
        }

        static CommentPageResponse buildCommentPageResponse(Page<Comment> pageComments, List<CommentResponse> commentDtos) {
            return CommentPageResponse.builder()
                    .commentDtos(commentDtos)
                    .currentPage(pageComments.getNumber())
                    .totalPages(pageComments.getTotalPages())
                    .totalElements(pageComments.getTotalElements())
                    .isLastPage(pageComments.isLast())
                    .build();
        }

        static PageRequest buildPageRequest(CommentsOnPostSearchRequest searchRequest) {
            return PageRequest.of(
                    searchRequest.getPage(),
                    searchRequest.getSize(),
                    Sort.by(Sort.Direction.DESC, CommentSortField.from(searchRequest.getSortBy()).getFieldName())
            );
        }
    }
}