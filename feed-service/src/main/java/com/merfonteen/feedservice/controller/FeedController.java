package com.merfonteen.feedservice.controller;

import com.merfonteen.feedservice.dto.FeedPageResponse;
import com.merfonteen.feedservice.dto.FeedSearchRequest;
import com.merfonteen.feedservice.dto.SubscriptionDto;
import com.merfonteen.feedservice.service.FeedService;
import com.merfonteen.feedservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/feed")
@RestController
public class FeedController {

    private final FeedService feedService;
    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<FeedPageResponse> getMyFeed(@RequestHeader("X-User-Id") Long currentUserId,
                                                      FeedSearchRequest feedSearchRequest) {
        return ResponseEntity.ok(feedService.getMyFeed(currentUserId, feedSearchRequest));
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
