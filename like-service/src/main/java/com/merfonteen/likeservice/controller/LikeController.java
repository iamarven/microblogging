package com.merfonteen.likeservice.controller;

import com.merfonteen.likeservice.dto.LikePageResponseDto;
import com.merfonteen.likeservice.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
