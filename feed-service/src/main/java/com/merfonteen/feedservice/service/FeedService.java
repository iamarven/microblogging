package com.merfonteen.feedservice.service;

import com.merfonteen.feedservice.dto.FeedPageResponseDto;
import com.merfonteen.feedservice.dto.event.PostCreatedEvent;
import com.merfonteen.feedservice.dto.event.PostRemovedEvent;

public interface FeedService {
    FeedPageResponseDto getMyFeed(Long currentUserId, int page, int size);
    void distributePostToSubscribers(PostCreatedEvent event);
    void deleteFeedsByPostId(PostRemovedEvent event);
}
