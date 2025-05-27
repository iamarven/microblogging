package com.merfonteen.feedservice.service;

import com.merfonteen.feedservice.dto.SubscriptionDto;

import java.util.List;

public interface SubscriptionService {
    List<SubscriptionDto> getMySubscriptions(Long currentUserId);
    List<SubscriptionDto> getMySubscribers(Long currentUserId);
    List<SubscriptionDto> getUserSubscribersByUserId(Long userId);
    SubscriptionDto follow(Long targetUserId, Long currentUserId);
    SubscriptionDto unfollow(Long targetUserId, Long currentUserId);
}
