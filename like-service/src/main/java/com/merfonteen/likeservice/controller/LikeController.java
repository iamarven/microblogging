package com.merfonteen.likeservice.controller;

import com.merfonteen.likeservice.dto.LikeResponse;
import com.merfonteen.likeservice.dto.LikePageResponse;
import com.merfonteen.likeservice.dto.LikesSearchRequest;
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
    public ResponseEntity<LikePageResponse> getLikesForPost(@PathVariable("id") Long postId,
                                                            LikesSearchRequest searchRequest) {
        return ResponseEntity.ok(likeService.getLikesForPost(postId, searchRequest));
    }

    @GetMapping("/posts/{id}/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable("id") Long postId) {
        return ResponseEntity.ok(likeService.getLikeCount(postId));
    }

    @PostMapping("/posts/{id}")
    public ResponseEntity<LikeResponse> likePost(@PathVariable("id") Long postId,
                                                 @RequestHeader("X-User-Id") Long currentUserId) {
        LikeResponse like = likeService.likePost(postId, currentUserId);
        URI location = URI.create("/api/likes/" + like.getId());
        return ResponseEntity.created(location).body(like);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<LikeResponse> removeLike(@PathVariable("id") Long postId,
                                                   @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(likeService.removeLike(postId, currentUserId));
    }
}
