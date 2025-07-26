package com.merfonteen.postservice.controller;

import com.merfonteen.dtos.FileUploadResponse;
import com.merfonteen.postservice.dto.PostCreateRequest;
import com.merfonteen.postservice.dto.PostResponse;
import com.merfonteen.postservice.dto.PostUpdateRequest;
import com.merfonteen.postservice.dto.PostsSearchRequest;
import com.merfonteen.postservice.dto.UserPostsPageResponse;
import com.merfonteen.postservice.service.PostMediaService;
import com.merfonteen.postservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RequestMapping("/api/posts")
@RestController
public class PostController {

    private final PostService postService;
    private final PostMediaService postMediaService;

    public PostController(PostService postService, PostMediaService postMediaService) {
        this.postService = postService;
        this.postMediaService = postMediaService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @GetMapping("/{id}/author-id")
    public ResponseEntity<Long> getPostAuthorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(postService.getPostAuthorId(id));
    }

    @GetMapping("/{id}/media")
    public ResponseEntity<List<String>> getMediaUrlsForPost(@PathVariable("id") Long id) {
        return ResponseEntity.ok(postMediaService.getMediaUrlsForPost(id));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserPostsPageResponse> getUserPosts(@PathVariable("userId") Long userId,
                                                              PostsSearchRequest request) {
        return ResponseEntity.ok(postService.getUserPosts(userId, request));
    }

    @GetMapping("/users/{id}/count")
    public ResponseEntity<Long> countPostsForUser(@PathVariable("id") Long userId) {
        return ResponseEntity.ok(postService.getPostCount(userId));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestHeader(name = "X-User-Id") Long currentUserId,
                                                   @RequestBody @Valid PostCreateRequest createDto) {
        PostResponse createdPost = postService.createPost(currentUserId, createDto);
        URI location = URI.create("/api/posts/" + createdPost.getId());
        return ResponseEntity.created(location).body(createdPost);
    }

    @PostMapping("/{id}/upload/media")
    public ResponseEntity<FileUploadResponse> uploadPostMedia(@PathVariable("id") Long postId,
                                                              @RequestParam(name = "file") MultipartFile file,
                                                              @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(postMediaService.uploadMediaToPost(postId, file, currentUserId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable("id") Long id,
                                                   @RequestBody @Valid PostUpdateRequest updateDto,
                                                   @RequestHeader(name = "X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(postService.updatePost(id, updateDto, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable("id") Long id,
                                                   @RequestHeader(name = "X-User-Id") Long currentUserId) {
        postService.deletePost(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/delete/media/{fileType}/{fileName}")
    public ResponseEntity<Void> deletePostMedia(@PathVariable("id") Long id,
                                                @PathVariable String fileType,
                                                @PathVariable String fileName,
                                                @RequestHeader(name = "X-User-Id") Long currentUserId) {
        postMediaService.deletePostMedia(id, fileType, fileName, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
