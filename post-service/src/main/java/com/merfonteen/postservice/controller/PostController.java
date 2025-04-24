package com.merfonteen.postservice.controller;

import com.merfonteen.postservice.dto.PostCreateDto;
import com.merfonteen.postservice.dto.PostResponseDto;
import com.merfonteen.postservice.service.PostService;
import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(@RequestHeader(name = "X-User-Id") Long currentUserId,
                                                      @RequestBody @Valid PostCreateDto createDto) {
        PostResponseDto createdPost = postService.createPost(currentUserId, createDto);
        URI location = URI.create("/api/posts/" + createdPost.getId());
        return ResponseEntity.created(location).body(createdPost);
    }
}
