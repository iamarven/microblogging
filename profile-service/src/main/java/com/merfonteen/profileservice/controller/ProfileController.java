package com.merfonteen.profileservice.controller;

import com.merfonteen.profileservice.dto.AggregatedProfileDto;
import com.merfonteen.profileservice.dto.CommentPageDto;
import com.merfonteen.profileservice.dto.CommentsSearchRequest;
import com.merfonteen.profileservice.dto.PostPageDto;
import com.merfonteen.profileservice.dto.PostsSearchRequest;
import com.merfonteen.profileservice.dto.ProfileSearchRequest;
import com.merfonteen.profileservice.service.CommentQueryService;
import com.merfonteen.profileservice.service.PostQueryService;
import com.merfonteen.profileservice.service.ProfileFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/profiles")
@RestController
public class ProfileController {
    private final ProfileFacade profileFacade;
    private final PostQueryService postQueryService;
    private final CommentQueryService commentQueryService;

    @GetMapping("/{userId}")
    public ResponseEntity<AggregatedProfileDto> getUserProfile(@PathVariable Long userId,
                                                               @Valid ProfileSearchRequest searchRequest) {

        return ResponseEntity.ok(profileFacade.getAggregatedProfile(userId, searchRequest));
    }

    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<PostPageDto> getUserPosts(@PathVariable Long userId,
                                                    @Valid PostsSearchRequest searchRequest) {

        return ResponseEntity.ok(postQueryService.getUserPosts(userId, searchRequest));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentPageDto> getCommentsOnPost(@PathVariable Long postId,
                                                            @Valid CommentsSearchRequest searchRequest) {

        return ResponseEntity.ok(commentQueryService.getComments(postId, searchRequest));
    }
}
