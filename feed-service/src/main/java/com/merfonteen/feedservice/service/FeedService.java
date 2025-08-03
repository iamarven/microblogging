package com.merfonteen.feedservice.service;

import com.merfonteen.feedservice.dto.FeedPageResponse;
import com.merfonteen.feedservice.dto.FeedSearchRequest;
import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;

public interface FeedService {
    FeedPageResponse getMyFeed(Long currentUserId, FeedSearchRequest request);
    void distributePostToSubscribers(PostCreatedEvent event);
    void deleteFeedsByPostId(PostRemovedEvent event);
}
