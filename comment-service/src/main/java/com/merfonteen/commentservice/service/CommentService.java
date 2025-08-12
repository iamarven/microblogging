package com.merfonteen.commentservice.service;

import com.merfonteen.commentservice.dto.*;
import com.merfonteen.kafkaEvents.PostRemovedEvent;

public interface CommentService {

    CommentPageResponse getCommentsOnPost(CommentsOnPostSearchRequest searchRequest);

    Long getCommentCountForPost(Long postId);

    CommentPageResponse getReplies(Long parentId, RepliesOnCommentSearchRequest searchRequest);

    Long getRepliesCountForComment(Long commentId);

    CommentResponse createComment(CommentCreateRequest requestDto, Long currentUserId);

    CommentResponse createReply(CommentReplyRequest replyRequestDto, Long currentUserId);

    CommentResponse updateComment(Long commentId, CommentUpdateRequest updateDto, Long currentUserId);

    void deleteComment(Long commentId, Long currentUserId);

    void removeCommentsOnPost(PostRemovedEvent event);
}
