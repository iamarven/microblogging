package com.merfonteen.feedservice.service;

import com.merfonteen.feedservice.dto.FeedDto;
import com.merfonteen.feedservice.dto.PostCreatedEvent;

import java.util.List;

public interface FeedService {
    List<FeedDto> getMyFeed(Long currentUserId);
    void distributePostToSubscribers(PostCreatedEvent event);
}
