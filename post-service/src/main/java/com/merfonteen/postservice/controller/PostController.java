package com.merfonteen.postservice.controller;

import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.dto.PostUpdateDto;
import com.merfonteen.postservice.dto.UserPostsPageResponseDto;
import com.merfonteen.postservice.model.enums.PostSortField;
import com.merfonteen.postservice.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RequestMapping("/api/posts")
@RestController
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPostById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @GetMapping("/{id}/author-id")
    public ResponseEntity<Long> getPostAuthorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(postService.getPostAuthorId(id));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserPostsPageResponseDto> getUserPosts(@PathVariable("userId") Long userId,
                                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                 @RequestParam(defaultValue = "10")@Min(1) int size,
                                                                 @RequestParam(defaultValue = "createdAt") String sortBy) {
        PostSortField postSortField = PostSortField.from(sortBy);
        return ResponseEntity.ok(postService.getUserPosts(userId, page, size, postSortField));
    }

    @GetMapping("/users/{id}/count")
    public ResponseEntity<Long> countPostsForUser(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(postService.getPostCount(userId));
    }

    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(@RequestHeader(name = "X-User-Id") Long currentUserId,
                                                      @RequestBody @Valid PostCreateDto createDto) {
        PostResponseDto createdPost = postService.createPost(currentUserId, createDto);
        URI location = URI.create("/api/posts/" + createdPost.getId());
        return ResponseEntity.created(location).body(createdPost);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PostResponseDto> updatePost(@PathVariable("id") Long id,
                                                      @RequestBody @Valid PostUpdateDto updateDto,
                                                      @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(postService.updatePost(id, updateDto, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<PostResponseDto> deletePost(@PathVariable("id") Long id,
                                                      @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(postService.deletePost(id, currentUserId));
    }
}
