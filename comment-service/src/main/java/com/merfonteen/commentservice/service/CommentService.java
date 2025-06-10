package com.merfonteen.commentservice.service;

import com.merfonteen.commentservice.dto.*;
import com.merfonteen.commentservice.model.enums.CommentSortField;
import com.merfonteen.kafkaEvents.PostRemovedEvent;

public interface CommentService {
    CommentPageResponseDto getCommentsOnPost(Long postId, int page, int size, CommentSortField sortField);
    Long getCommentCountForPost(Long postId);
    CommentPageResponseDto getReplies(Long parentId, int page, int size);
    CommentResponseDto createComment(CommentRequestDto requestDto, Long currentUserId);
    CommentResponseDto createReply(CommentReplyRequestDto replyRequestDto, Long currentUserId);
    CommentResponseDto updateComment(Long commentId, CommentUpdateDto updateDto, Long currentUserId);
    CommentResponseDto deleteComment(Long commentId, Long currentUserId);
    void removeCommentsOnPost(PostRemovedEvent event);
}
