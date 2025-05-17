package com.merfonteen.likeservice.controller;

import com.merfonteen.likeservice.dto.LikeDto;
import com.merfonteen.likeservice.dto.LikePageResponseDto;
import com.merfonteen.likeservice.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RequestMapping("/api/likes")
@RestController
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<LikePageResponseDto> getLikesForPost(@PathVariable("id") Long postId,
                                                               @RequestParam(required = false, defaultValue = "0") int page,
                                                               @RequestParam(required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(likeService.getLikesForPost(postId, page, size));
    }

    @GetMapping("/posts/{id}/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable("id") Long postId) {
        return ResponseEntity.ok(likeService.getLikeCount(postId));
    }

    @PostMapping("/posts/{id}")
    public ResponseEntity<LikeDto> likePost(@PathVariable("id") Long postId,
                                        @RequestHeader("X-User-Id") Long currentUserId) {
        LikeDto like = likeService.likePost(postId, currentUserId);
        URI location = URI.create("/api/likes/" + like.getId());
        return ResponseEntity.created(location).body(like);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<LikeDto> removeLike(@PathVariable("id") Long postId,
                                              @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(likeService.removeLike(postId, currentUserId));
    }
}
