package com.merfonteen.commentservice.service.impl;

import com.merfonteen.commentservice.client.PostClient;
import com.merfonteen.commentservice.dto.CommentCreateRequest;
import com.merfonteen.commentservice.dto.CommentPageResponse;
import com.merfonteen.commentservice.dto.CommentReplyRequest;
import com.merfonteen.commentservice.dto.CommentResponse;
import com.merfonteen.commentservice.dto.CommentUpdateRequest;
import com.merfonteen.commentservice.dto.CommentsOnPostSearchRequest;
import com.merfonteen.commentservice.dto.RepliesOnCommentSearchRequest;
import com.merfonteen.commentservice.mapper.CommentMapper;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.model.enums.OutboxEventType;
import com.merfonteen.commentservice.repository.CommentRepository;
import com.merfonteen.commentservice.service.CommentService;
import com.merfonteen.commentservice.service.OutboxService;
import com.merfonteen.commentservice.service.redis.CommentRateLimiter;
import com.merfonteen.commentservice.service.redis.RedisCacheInvalidator;
import com.merfonteen.commentservice.service.redis.RedisCounter;
import com.merfonteen.commentservice.util.AuthUtil;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    private final OutboxService outboxService;
    private final CommentRepository commentRepository;
    private final CommentRateLimiter commentRateLimiter;
    private final RedisCacheInvalidator redisCacheInvalidator;

    @Cacheable(value = COMMENTS_BY_POST_ID_CACHE, key = "#searchRequest.getPostId() + " +
                                                        "':' + #searchRequest.page + " +
                                                        "':' + #searchRequest.size + " +
                                                        "':' + #searchRequest.sortBy")
    @Override
    public CommentPageResponse getCommentsOnPost(CommentsOnPostSearchRequest searchRequest) {
        PageRequest pageRequest = commentMapper.buildPageRequest(searchRequest);
        Page<Comment> commentsPage = commentRepository.findAllByPostId(searchRequest.getPostId(), pageRequest);
        List<CommentResponse> commentDtos = commentMapper.toDtos(commentsPage.getContent());

        return commentMapper.buildCommentPageResponse(commentsPage, commentDtos);
    }

    @Override
    public Long getCommentCountForPost(Long postId) {
        String cachedValue = redisCounter.getCachedValue(redisCounter.getCommentsCacheKey(postId));

        if (cachedValue != null) {
            return Long.parseLong(cachedValue);
        }

        long countFromDb = commentRepository.countAllByPostId(postId);
        redisCounter.setCounter(redisCounter.getCommentsCacheKey(postId), countFromDb);

        return countFromDb;
    }

    @Cacheable(value = COMMENT_REPLIES_CACHE, key = "#parentId + " +
                                                    "':' + #searchRequest.page + " +
                                                    "':' + #searchRequest.size")
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
        String cachedValue = redisCounter.getCachedValue(redisCounter.getRepliesCacheKey(commentId));

        if (cachedValue != null) {
            return Long.parseLong(cachedValue);
        }

        long countFromDb = commentRepository.countAllByParentId(commentId);
        redisCounter.setCounter(redisCounter.getRepliesCacheKey(commentId), countFromDb);

        return countFromDb;
    }

    @Transactional
    @Override
    public CommentResponse createComment(CommentCreateRequest requestDto, Long currentUserId) {
        checkPostExistsOrThrowException(requestDto.getPostId());

        commentRateLimiter.limitLeavingComments(currentUserId);

        Comment comment = Comment.builder()
                .postId(requestDto.getPostId())
                .userId(currentUserId)
                .content(requestDto.getContent())
                .createdAt(Instant.now())
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment '{}' saved to database successfully", savedComment.getId());

        outboxService.create(savedComment, OutboxEventType.COMMENT_CREATED);

        redisCacheInvalidator.evictPostsCache(requestDto.getPostId());
        redisCounter.incrementCounter(redisCounter.getCommentsCacheKey(savedComment.getPostId()));

        return commentMapper.toDto(savedComment);
    }

    @Transactional
    @Override
    public CommentResponse createReply(CommentReplyRequest replyRequest, Long currentUserId) {
        Comment parent = getCommentByIdOrThrowException(replyRequest.getParentId());

        commentRateLimiter.limitLeavingComments(currentUserId);

        Comment reply = Comment.builder()
                .parent(parent)
                .content(replyRequest.getContent())
                .postId(parent.getPostId())
                .userId(currentUserId)
                .createdAt(Instant.now())
                .build();

        Comment saved = commentRepository.save(reply);
        log.info("Added nested comment '{}' to comment '{}'", saved.getId(), parent.getId());

        redisCacheInvalidator.evictPostsCache(parent.getPostId());
        redisCacheInvalidator.evictRepliesCache(parent.getId());

        redisCounter.incrementCounter(redisCounter.getRepliesCacheKey(parent.getId()));

        return commentMapper.toDto(saved);
    }

    @Transactional
    @Override
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest updateDto, Long currentUserId) {
        Comment commentToUpdate = getCommentByIdOrThrowException(commentId);

        AuthUtil.validateChangingComment(currentUserId, commentToUpdate.getUserId());

        commentToUpdate.setContent(updateDto.getContent());
        commentToUpdate.setUpdatedAt(Instant.now());

        Comment savedComment = commentRepository.save(commentToUpdate);
        log.info("Comment '{}' was successfully updated", savedComment.getId());

        redisCacheInvalidator.evictPostsCache(savedComment.getPostId());

        return commentMapper.toDto(savedComment);
    }

    @Transactional
    @Override
    public void deleteComment(Long commentId, Long currentUserId) {
        Comment comment = getCommentByIdOrThrowException(commentId);
        AuthUtil.validateChangingComment(currentUserId, comment.getUserId());

        commentRepository.delete(comment);
        log.info("User '{}' deleted comment '{}'", currentUserId, commentId);

        outboxService.create(comment, OutboxEventType.COMMENT_REMOVED);

        redisCacheInvalidator.evictPostsCache(comment.getPostId());
        if (comment.getParent() != null) {
            redisCacheInvalidator.evictRepliesCache(comment.getParent().getId());
        }
        redisCounter.decrementCounter(redisCounter.getCommentsCacheKey(comment.getPostId()));
    }

    @Transactional
    @Override
    public void removeCommentsOnPost(PostRemovedEvent event) {
        int count = commentRepository.deleteAllByPostId(event.getPostId());
        log.info("Deleted {} comments for postId={}", count, event.getPostId());
        redisCacheInvalidator.evictPostsCache(event.getPostId());
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
