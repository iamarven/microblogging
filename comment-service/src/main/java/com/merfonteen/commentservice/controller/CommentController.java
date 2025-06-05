package com.merfonteen.commentservice.controller;

import com.merfonteen.commentservice.dto.CommentPageResponseDto;
import com.merfonteen.commentservice.dto.CommentRequestDto;
import com.merfonteen.commentservice.dto.CommentResponseDto;
import com.merfonteen.commentservice.model.enums.CommentSortField;
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

    @GetMapping("/{id}")
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

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@Valid @RequestBody CommentRequestDto requestDto,
                                                            @RequestHeader(name = "X-User-Id") Long currentUserId) {
        CommentResponseDto comment = commentService.createComment(requestDto, currentUserId);
        URI location = URI.create("/api/comments/" + comment.getId());
        return ResponseEntity.created(location).body(comment);
    }
}
