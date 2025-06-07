package com.merfonteen.commentservice.service;

import com.merfonteen.commentservice.dto.CommentPageResponseDto;
import com.merfonteen.commentservice.dto.CommentRequestDto;
import com.merfonteen.commentservice.dto.CommentResponseDto;
import com.merfonteen.commentservice.dto.CommentUpdateDto;
import com.merfonteen.commentservice.model.enums.CommentSortField;
import com.merfonteen.kafkaEvents.PostRemovedEvent;

public interface CommentService {
    CommentPageResponseDto getCommentsOnPost(Long postId, int page, int size, CommentSortField sortField);
    Long getCommentCountForPost(Long postId);
    CommentResponseDto createComment(CommentRequestDto requestDto, Long currentUserId);
    CommentResponseDto updateComment(Long commentId, CommentUpdateDto updateDto, Long currentUserId);
    CommentResponseDto deleteComment(Long commentId, Long currentUserId);
    void removeCommentsOnPost(PostRemovedEvent event);
}
