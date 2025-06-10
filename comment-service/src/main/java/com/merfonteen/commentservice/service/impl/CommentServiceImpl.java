package com.merfonteen.commentservice.service.impl;

import com.merfonteen.commentservice.client.PostClient;
import com.merfonteen.commentservice.dto.*;
import com.merfonteen.commentservice.kafka.eventProducer.CommentEventProducer;
import com.merfonteen.commentservice.mapper.CommentMapper;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.model.enums.CommentSortField;
import com.merfonteen.commentservice.repository.CommentRepository;
import com.merfonteen.commentservice.service.CommentService;
import com.merfonteen.commentservice.util.AuthUtil;
import com.merfonteen.commentservice.util.CommentRateLimiter;
import com.merfonteen.commentservice.util.RedisCacheCleaner;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Primary
@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {

    private final PostClient postClient;
    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final RedisCacheCleaner redisCacheCleaner;
    private final CommentRateLimiter commentRateLimiter;
    private final StringRedisTemplate stringRedisTemplate;
    private final CommentEventProducer commentEventProducer;

    @Cacheable(value = "comments-by-postId", key = "#postId + ':' + #page + ':' + #size")
    @Override
    public CommentPageResponseDto getCommentsOnPost(Long postId, int page, int size, CommentSortField sortField) {
        if(size > 100) {
            size = 100;
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField.getFieldName()));
        Page<Comment> commentsPage = commentRepository.findAllByPostId(postId, pageRequest);
        List<CommentResponseDto> commentDtos = commentMapper.toDtos(commentsPage.getContent());

        return CommentPageResponseDto.builder()
                .commentDtos(commentDtos)
                .currentPage(commentsPage.getNumber())
                .totalPages(commentsPage.getTotalPages())
                .totalElements(commentsPage.getTotalElements())
                .isLastPage(commentsPage.isLast())
                .build();
    }

    @Override
    public Long getCommentCountForPost(Long postId) {
        String cacheKey = "comment:count:post:" + postId;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);

        if(cacheValue != null) {
            return Long.parseLong(cacheValue);
        }

        long countFromDb = commentRepository.countAllByPostId(postId);
        stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(countFromDb), Duration.ofMinutes(10));

        return countFromDb;
    }

    @Cacheable(value = "comment-replies", key = "#parentId + ':' + #page + ':' + #size")
    @Override
    public CommentPageResponseDto getReplies(Long parentId, int page, int size) {
        findCommentByIdOrThrowException(parentId);

        if(size > 100) {
            size = 100;
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> replies = commentRepository.findAllByParentId(parentId, pageRequest);
        List<CommentResponseDto> repliesPage = commentMapper.toDtos(replies.getContent());

        return CommentPageResponseDto.builder()
                .commentDtos(repliesPage)
                .currentPage(replies.getNumber())
                .totalPages(replies.getTotalPages())
                .totalElements(replies.getTotalElements())
                .isLastPage(replies.isLast())
                .build();
    }

    @Transactional
    @Override
    public CommentResponseDto createComment(CommentRequestDto requestDto, Long currentUserId) {
        checkPostExistsOrThrowException(requestDto.getPostId());

        Comment comment = Comment.builder()
                .postId(requestDto.getPostId())
                .userId(currentUserId)
                .content(requestDto.getContent())
                .createdAt(Instant.now())
                .build();

        commentRateLimiter.limitLeavingComments(currentUserId);

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment '{}' saved to database successfully", savedComment.getId());

        CommentCreatedEvent commentCreatedEvent = CommentCreatedEvent.builder()
                .commentId(savedComment.getId())
                .userId(currentUserId)
                .postId(requestDto.getPostId())
                .build();

        commentEventProducer.sendCommentCreatedEvent(commentCreatedEvent);

        redisCacheCleaner.evictCommentCacheOnPostByPostId(requestDto.getPostId());
        stringRedisTemplate.opsForValue().increment("comment:count:post:" + savedComment.getPostId());

        return commentMapper.toDto(savedComment);
    }

    @Transactional
    @Override
    public CommentResponseDto createReply(CommentReplyRequestDto replyRequestDto, Long currentUserId) {
        Comment parent = findCommentByIdOrThrowException(replyRequestDto.getParentId());

        Comment reply = Comment.builder()
                .parent(parent)
                .content(replyRequestDto.getContent())
                .postId(parent.getPostId())
                .userId(currentUserId)
                .createdAt(Instant.now())
                .build();

        commentRateLimiter.limitLeavingComments(currentUserId);

        Comment saved = commentRepository.save(reply);
        log.info("Added nested comment '{}' to comment '{}'", saved.getId(), parent.getId());

        redisCacheCleaner.evictCommentCacheOnPostByPostId(parent.getPostId());
        redisCacheCleaner.evictCommentRepliesCacheByParentId(replyRequestDto.getParentId());
        stringRedisTemplate.opsForValue().increment("comment:count:post:" + parent.getPostId());

        return commentMapper.toDto(saved);
    }

    @Transactional
    @Override
    public CommentResponseDto updateComment(Long commentId, CommentUpdateDto updateDto, Long currentUserId) {
        Comment commentToUpdate = findCommentByIdOrThrowException(commentId);

        AuthUtil.validateChangingComment(currentUserId, commentToUpdate.getUserId());

        commentToUpdate.setContent(updateDto.getContent());
        commentToUpdate.setUpdatedAt(Instant.now());

        Comment savedComment = commentRepository.save(commentToUpdate);
        log.info("Comment '{}' was successfully updated", savedComment.getId());

        redisCacheCleaner.evictCommentCacheOnPostByPostId(commentToUpdate.getPostId());

        return commentMapper.toDto(savedComment);
    }

    @Transactional
    @Override
    public CommentResponseDto deleteComment(Long commentId, Long currentUserId) {
        Comment comment = findCommentByIdOrThrowException(commentId);
        AuthUtil.validateChangingComment(currentUserId, comment.getUserId());

        commentRepository.delete(comment);
        log.info("User '{}' deleted comment '{}'", currentUserId, commentId);

        CommentRemovedEvent commentRemovedEvent = CommentRemovedEvent.builder()
                .commentId(commentId)
                .userId(currentUserId)
                .postId(comment.getPostId())
                .build();

        commentEventProducer.sendCommentRemovedEvent(commentRemovedEvent);

        redisCacheCleaner.evictCommentCacheOnPostByPostId(comment.getPostId());
        stringRedisTemplate.opsForValue().decrement("comment:count:post:" + comment.getPostId());

        return commentMapper.toDto(comment);
    }

    @Transactional
    @Override
    public void removeCommentsOnPost(PostRemovedEvent event) {
        int count = commentRepository.deleteAllByPostId(event.getPostId());
        log.info("Deleted {} comments for postId={}", count, event.getPostId());
    }

    private Comment findCommentByIdOrThrowException(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("There was not found comment '%d'", commentId)));
    }

    private void checkPostExistsOrThrowException(Long postId) {
        try {
            postClient.checkPostExists(postId);
        } catch (FeignException.NotFound e) {
            log.warn("Exception during checking post exists with id '{}'", postId);
            throw new NotFoundException(String.format("Post with id '%d' not found", postId));
        }
    }
}
