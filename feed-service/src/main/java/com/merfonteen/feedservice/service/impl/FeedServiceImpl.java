package com.merfonteen.feedservice.service.impl;

import com.merfonteen.feedservice.dto.FeedDto;
import com.merfonteen.feedservice.dto.PostCreatedEvent;
import com.merfonteen.feedservice.mapper.FeedMapper;
import com.merfonteen.feedservice.model.Feed;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.repository.FeedRepository;
import com.merfonteen.feedservice.repository.SubscriptionRepository;
import com.merfonteen.feedservice.service.FeedService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Primary
@Service
public class FeedServiceImpl implements FeedService {

    private final FeedMapper feedMapper;
    private final FeedRepository feedRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public List<FeedDto> getMyFeed(Long currentUserId) {
        List<Feed> feedForUser = feedRepository.findAllByUserId(currentUserId);
        return feedForUser.stream()
                .map(feedMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public void distributePostToSubscribers(PostCreatedEvent event) {
        List<Subscription> subscriptions = subscriptionRepository.findAllByFolloweeId(event.getAuthorId());
        List<Feed> buffer = new ArrayList<>();

        for(Subscription subscription : subscriptions) {
            buffer.add(Feed.builder()
                    .postId(event.getPostId())
                    .userId(subscription.getFollowerId())
                    .createdAt(event.getCreatedAt())
                    .build());

            if(buffer.size() == 50) {
                safeSaveFeeds(buffer);
                buffer.clear();
            }
        }

        if(!buffer.isEmpty()) {
            safeSaveFeeds(buffer);
        }
    }

    private void safeSaveFeeds(List<Feed> feedsToSave) {
        try {
            feedRepository.saveAll(feedsToSave);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicated feed entry detected, skipping duplicates: {}", e.getMessage());
        }
    }
}
