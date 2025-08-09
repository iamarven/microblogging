package com.merfonteen.commentservice.service.impl;

import com.merfonteen.commentservice.client.PostClient;
import com.merfonteen.commentservice.dto.CommentPageResponse;
import com.merfonteen.commentservice.dto.CommentReplyRequest;
import com.merfonteen.commentservice.dto.CommentCreateRequest;
import com.merfonteen.commentservice.dto.CommentResponse;
import com.merfonteen.commentservice.dto.CommentUpdateRequest;
import com.merfonteen.commentservice.dto.CommentsOnPostSearchRequest;
import com.merfonteen.commentservice.dto.RepliesOnCommentSearchRequest;
import com.merfonteen.commentservice.kafka.eventProducer.CommentEventProducer;
import com.merfonteen.commentservice.mapper.CommentMapper;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.repository.CommentRepository;
import com.merfonteen.commentservice.service.CommentService;
import com.merfonteen.commentservice.service.redis.RedisCounter;
import com.merfonteen.commentservice.util.AuthUtil;
import com.merfonteen.commentservice.service.redis.CommentRateLimiter;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static com.merfonteen.commentservice.config.CacheNames.COMMENTS_BY_POST_ID_CACHE;
import static com.merfonteen.commentservice.config.CacheNames.COMMENT_REPLIES_CACHE;

@Slf4j
@Primary
@RequiredArgsConstructor
@Service
public class CommentServiceImpl implements CommentService {
    private final PostClient postClient;
    private final RedisCounter redisCounter;
    private final CommentMapper commentMapper;
    private final CommentRepository commentRepository;
    private final CommentRateLimiter commentRateLimiter;
    private final CommentEventProducer commentEventProducer;

    @Cacheable(value = COMMENTS_BY_POST_ID_CACHE, key = "#searchRequest.getPostId()")
    @Override
    public CommentPageResponse getCommentsOnPost(CommentsOnPostSearchRequest searchRequest) {
        PageRequest pageRequest = commentMapper.buildPageRequest(searchRequest);
        Page<Comment> commentsPage = commentRepository.findAllByPostId(searchRequest.getPostId(), pageRequest);
        List<CommentResponse> commentDtos = commentMapper.toDtos(commentsPage.getContent());

        return commentMapper.buildCommentPageResponse(commentsPage, commentDtos);
    }

    @Override
    public Long getCommentCountForPost(Long postId) {
        String cacheValue = redisCounter.getCachedValue(redisCounter.getCommentsCacheKey(postId));

        if(cacheValue != null) {
            return Long.parseLong(cacheValue);
        }

        long countFromDb = commentRepository.countAllByPostId(postId);
        redisCounter.setCounter(redisCounter.getCommentsCacheKey(postId), countFromDb);

        return countFromDb;
    }

    @Cacheable(value = COMMENT_REPLIES_CACHE, key = "#parentId")
    @Override
    public CommentPageResponse getReplies(Long parentId, RepliesOnCommentSearchRequest searchRequest) {
        getCommentByIdOrThrowException(parentId);

        PageRequest pageRequest = commentMapper.buildPageRequest(searchRequest);
        Page<Comment> replies = commentRepository.findAllByParentId(parentId, pageRequest);
        List<CommentResponse> repliesPage = commentMapper.toDtos(replies.getContent());

        return commentMapper.buildCommentPageResponse(replies, repliesPage);
    }

    @Override
    public Long getRepliesCountForComment(Long commentId) {
        String cachedValue = redisCounter.getRepliesCacheKey(commentId);

        if(cachedValue != null) {
            return Long.parseLong(cachedValue);
        }

        long countFromDb = commentRepository.countAllByParentId(commentId);
        redisCounter.setCounter(redisCounter.getRepliesCacheKey(commentId), countFromDb);

        return countFromDb;
    }

    @CacheEvict(value = COMMENTS_BY_POST_ID_CACHE, key = "#requestDto.getPostId()")
    @Transactional
    @Override
    public CommentResponse createComment(CommentCreateRequest requestDto, Long currentUserId) {
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

        redisCounter.incrementCounter(redisCounter.getCommentsCacheKey(savedComment.getPostId()));

        return commentMapper.toDto(savedComment);
    }

    @Caching(evict = {
            @CacheEvict(value = COMMENTS_BY_POST_ID_CACHE, key = "#result.getPostId()"),
            @CacheEvict(value = COMMENT_REPLIES_CACHE, key = "#replyRequest.getParentId()")
    })
    @Transactional
    @Override
    public CommentResponse createReply(CommentReplyRequest replyRequest, Long currentUserId) {
        Comment parent = getCommentByIdOrThrowException(replyRequest.getParentId());

        Comment reply = Comment.builder()
                .parent(parent)
                .content(replyRequest.getContent())
                .postId(parent.getPostId())
                .userId(currentUserId)
                .createdAt(Instant.now())
                .build();

        commentRateLimiter.limitLeavingComments(currentUserId);

        Comment saved = commentRepository.save(reply);
        log.info("Added nested comment '{}' to comment '{}'", saved.getId(), parent.getId());

        redisCounter.incrementCounter(redisCounter.getRepliesCacheKey(saved.getId()));

        return commentMapper.toDto(saved);
    }

    @CacheEvict(value = COMMENTS_BY_POST_ID_CACHE, key = "#result.getPostId()")
    @Transactional
    @Override
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest updateDto, Long currentUserId) {
        Comment commentToUpdate = getCommentByIdOrThrowException(commentId);

        AuthUtil.validateChangingComment(currentUserId, commentToUpdate.getUserId());

        commentToUpdate.setContent(updateDto.getContent());
        commentToUpdate.setUpdatedAt(Instant.now());

        Comment savedComment = commentRepository.save(commentToUpdate);
        log.info("Comment '{}' was successfully updated", savedComment.getId());

        return commentMapper.toDto(savedComment);
    }

    @Caching(evict = {
            @CacheEvict(value = COMMENTS_BY_POST_ID_CACHE, key = "#result.getPostId()"),
            @CacheEvict(value = COMMENT_REPLIES_CACHE, key = "#result.getParentId()")
    })
    @Transactional
    @Override
    public CommentResponse deleteComment(Long commentId, Long currentUserId) {
        Comment comment = getCommentByIdOrThrowException(commentId);
        AuthUtil.validateChangingComment(currentUserId, comment.getUserId());

        commentRepository.delete(comment);
        log.info("User '{}' deleted comment '{}'", currentUserId, commentId);

        CommentRemovedEvent commentRemovedEvent = CommentRemovedEvent.builder()
                .commentId(commentId)
                .userId(currentUserId)
                .postId(comment.getPostId())
                .build();

        commentEventProducer.sendCommentRemovedEvent(commentRemovedEvent);

        redisCounter.decrementCounter(redisCounter.getCommentsCacheKey(comment.getPostId()));

        return commentMapper.toDto(comment);
    }

    // TO DO: update reply

    // TO DO: delete reply

    @CacheEvict(value = COMMENTS_BY_POST_ID_CACHE, key = "#event.getPostId()")
    @Transactional
    @Override
    public void removeCommentsOnPost(PostRemovedEvent event) {
        int count = commentRepository.deleteAllByPostId(event.getPostId());
        log.info("Deleted {} comments for postId={}", count, event.getPostId());
    }

    private Comment getCommentByIdOrThrowException(Long commentId) {
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
