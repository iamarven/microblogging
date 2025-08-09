package com.merfonteen.commentservice.controller;

import com.merfonteen.commentservice.dto.*;
import com.merfonteen.commentservice.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RequestMapping("/api/comments")
@RequiredArgsConstructor
@RestController
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/posts")
    public ResponseEntity<CommentPageResponse> getCommentsOnPost(CommentsOnPostSearchRequest searchRequest) {
        return ResponseEntity.ok(commentService.getCommentsOnPost(searchRequest));
    }

    @GetMapping("/{id}/replies")
    public ResponseEntity<CommentPageResponse> getReplies(@PathVariable("id") Long parentId,
                                                         RepliesOnCommentSearchRequest searchRequest) {
        return ResponseEntity.ok(commentService.getReplies(parentId, searchRequest));
    }

    @GetMapping("/posts/{id}/count")
    public ResponseEntity<Long> getCommentCountForPost(@PathVariable("id") Long postId) {
        return ResponseEntity.ok(commentService.getCommentCountForPost(postId));
    }

    @GetMapping("/{id}/replies/count")
    public ResponseEntity<Long> getRepliesCountForComment(@PathVariable("id") Long commentId) {
        return ResponseEntity.ok(commentService.getRepliesCountForComment(commentId));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@RequestBody @Valid CommentCreateRequest requestDto,
                                                         @RequestHeader(name = "X-User-Id") Long currentUserId) {
        CommentResponse comment = commentService.createComment(requestDto, currentUserId);
        URI location = URI.create("/api/comments/" + comment.getId());
        return ResponseEntity.created(location).body(comment);
    }

    @PostMapping("/replies")
    public ResponseEntity<CommentResponse> createReply(@Valid @RequestBody CommentReplyRequest replyRequestDto,
                                                       @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(commentService.createReply(replyRequestDto, currentUserId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable("id") Long commentId,
                                                         @RequestBody @Valid CommentUpdateRequest updateDto,
                                                         @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(commentService.updateComment(commentId, updateDto, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommentResponse> deleteComment(@PathVariable("id") Long commentId,
                                                         @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(commentService.deleteComment(commentId, currentUserId));
    }
}
