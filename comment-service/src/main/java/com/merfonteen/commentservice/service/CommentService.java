package com.merfonteen.commentservice.service;

import com.merfonteen.commentservice.dto.CommentPageResponseDto;
import com.merfonteen.commentservice.dto.CommentRequestDto;
import com.merfonteen.commentservice.dto.CommentResponseDto;
import com.merfonteen.commentservice.dto.CommentUpdateDto;
import com.merfonteen.commentservice.model.enums.CommentSortField;

public interface CommentService {
    CommentPageResponseDto getCommentsOnPost(Long postId, int page, int size, CommentSortField sortField);
    CommentResponseDto createComment(CommentRequestDto requestDto, Long currentUserId);
    CommentResponseDto updateComment(Long commentId, CommentUpdateDto updateDto, Long currentUserId);
    CommentResponseDto deleteComment(Long commentId, Long currentUserId);
}
