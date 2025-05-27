package com.merfonteen.feedservice.controller;

import com.merfonteen.feedservice.dto.FeedPageResponseDto;
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
    public ResponseEntity<FeedPageResponseDto> getMyFeed(@RequestHeader("X-User-Id") Long currentUserId,
                                                         @RequestParam(required = false, defaultValue = "0") int page,
                                                         @RequestParam(required = false, defaultValue = "10") int size) {
        return ResponseEntity.ok(feedService.getMyFeed(currentUserId, page, size));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDto>> getMySubscriptions(@RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(currentUserId));
    }

    @GetMapping("/subscribers")
    public ResponseEntity<List<SubscriptionDto>> getMySubscribers(@RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(subscriptionService.getMySubscribers(currentUserId));
    }

    @GetMapping("/subscribers/users/{id}")
    public ResponseEntity<List<SubscriptionDto>> getUserSubscribers(@PathVariable("id") Long id) {
        return ResponseEntity.ok(subscriptionService.getUserSubscribersByUserId(id));
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
