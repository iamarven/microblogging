package com.merfonteen.feedservice.controller;

import com.merfonteen.feedservice.dto.FeedDto;
import com.merfonteen.feedservice.dto.SubscriptionDto;
import com.merfonteen.feedservice.service.FeedService;
import com.merfonteen.feedservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/feed")
@RestController
public class FeedController {

    private final FeedService feedService;
    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<List<FeedDto>> getMyFeed(@RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(feedService.getMyFeed(currentUserId));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDto>> getMySubscriptions(@RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(currentUserId));
    }

    @PostMapping("/follow/{targetUserId}")
    public ResponseEntity<SubscriptionDto> follow(@PathVariable("targetUserId") Long targetUserId,
                                                  @RequestHeader("X-User-Id") Long currentUserId) {
        SubscriptionDto subscriptionDto = subscriptionService.follow(targetUserId, currentUserId);
        URI location = URI.create("/api/feed/subscriptions/" + subscriptionDto.getId());
        return ResponseEntity.created(location).body(subscriptionDto);
    }

    @DeleteMapping("/unfollow/{targetUserId}")
    public ResponseEntity<SubscriptionDto> unfollow(@PathVariable("targetUserId") Long targetUserId,
                                                    @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(subscriptionService.unfollow(targetUserId, currentUserId));
    }
}
