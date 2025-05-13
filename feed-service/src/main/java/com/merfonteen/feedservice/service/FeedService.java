package com.merfonteen.feedservice.service;

import com.merfonteen.feedservice.dto.FeedPageResponseDto;
import com.merfonteen.feedservice.dto.event.PostCreatedEvent;

public interface FeedService {
    FeedPageResponseDto getMyFeed(Long currentUserId, int page, int size);
    void distributePostToSubscribers(PostCreatedEvent event);
}
