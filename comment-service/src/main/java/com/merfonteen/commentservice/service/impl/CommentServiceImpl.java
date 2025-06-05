package com.merfonteen.commentservice.service.impl;

import com.merfonteen.commentservice.client.PostClient;
import com.merfonteen.commentservice.dto.CommentPageResponseDto;
import com.merfonteen.commentservice.dto.CommentRequestDto;
import com.merfonteen.commentservice.dto.CommentResponseDto;
import com.merfonteen.commentservice.dto.CommentUpdateDto;
import com.merfonteen.commentservice.mapper.CommentMapper;
import com.merfonteen.commentservice.model.Comment;
import com.merfonteen.commentservice.model.enums.CommentSortField;
import com.merfonteen.commentservice.repository.CommentRepository;
import com.merfonteen.commentservice.service.CommentService;
import com.merfonteen.commentservice.util.AuthUtil;
import com.merfonteen.commentservice.util.CommentRateLimiter;
import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.ForbiddenException;
import com.merfonteen.exceptions.NotFoundException;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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
    private final CommentRateLimiter commentRateLimiter;

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

        return commentMapper.toDto(savedComment);
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

        return commentMapper.toDto(savedComment);
    }

    @Transactional
    @Override
    public CommentResponseDto deleteComment(Long commentId, Long currentUserId) {
        Comment comment = findCommentByIdOrThrowException(commentId);
        AuthUtil.validateChangingComment(currentUserId, comment.getUserId());

        commentRepository.delete(comment);
        log.info("User '{}' deleted comment '{}'", currentUserId, commentId);

        return commentMapper.toDto(comment);
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
