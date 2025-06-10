package com.merfonteen.commentservice.controller;

import com.merfonteen.commentservice.dto.*;
import com.merfonteen.commentservice.model.enums.CommentSortField;
import com.merfonteen.commentservice.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequestMapping("/api/comments")
@RequiredArgsConstructor
@RestController
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/posts/{id}")
    public ResponseEntity<CommentPageResponseDto> getCommentsOnPost(@PathVariable("id")
                                                                    Long postId,
                                                                    @RequestParam(required = false, defaultValue = "0")
                                                                    int page,
                                                                    @RequestParam(required = false, defaultValue = "10")
                                                                    int size,
                                                                    @RequestParam(required = false, defaultValue = "createdAt")
                                                                    String sortBy) {
        CommentSortField sortField = CommentSortField.from(sortBy);
        return ResponseEntity.ok(commentService.getCommentsOnPost(postId, page, size, sortField));
    }

    @GetMapping("/{id}/replies")
    public ResponseEntity<CommentPageResponseDto> getReplies(@PathVariable("id") Long parentId,
                                                               @RequestParam(required = false, defaultValue = "0") int page,
                                                               @RequestParam(required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(commentService.getReplies(parentId, page, size));
    }

    @GetMapping("/posts/{id}/count")
    public ResponseEntity<Long> getCommentCountForPost(@PathVariable("id") Long postId) {
        return ResponseEntity.ok(commentService.getCommentCountForPost(postId));
    }

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@RequestBody @Valid CommentRequestDto requestDto,
                                                            @RequestHeader(name = "X-User-Id") Long currentUserId) {
        CommentResponseDto comment = commentService.createComment(requestDto, currentUserId);
        URI location = URI.create("/api/comments/" + comment.getId());
        return ResponseEntity.created(location).body(comment);
    }

    @PostMapping("/replies")
    public ResponseEntity<CommentResponseDto> createReply(@Valid @RequestBody CommentReplyRequestDto replyRequestDto,
                                                          @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(commentService.createReply(replyRequestDto, currentUserId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CommentResponseDto> updateComment(@PathVariable("id") Long commentId,
                                                            @RequestBody @Valid CommentUpdateDto updateDto,
                                                            @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(commentService.updateComment(commentId, updateDto, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommentResponseDto> deleteComment(@PathVariable("id") Long commentId,
                                                            @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(commentService.deleteComment(commentId, currentUserId));
    }
}
