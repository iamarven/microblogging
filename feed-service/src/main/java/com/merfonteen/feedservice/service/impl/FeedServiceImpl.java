package com.merfonteen.feedservice.service.impl;

import com.merfonteen.feedservice.config.CacheNames;
import com.merfonteen.feedservice.dto.FeedDto;
import com.merfonteen.feedservice.dto.FeedPageResponse;
import com.merfonteen.feedservice.dto.FeedSearchRequest;
import com.merfonteen.feedservice.mapper.FeedMapper;
import com.merfonteen.feedservice.model.Feed;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.repository.FeedRepository;
import com.merfonteen.feedservice.repository.SubscriptionRepository;
import com.merfonteen.feedservice.service.FeedCacheInvalidator;
import com.merfonteen.feedservice.service.FeedService;
import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Primary
@Service
public class FeedServiceImpl implements FeedService {
    private final FeedMapper feedMapper;
    private final FeedRepository feedRepository;
    private final FeedCacheInvalidator feedCacheInvalidator;
    private final SubscriptionRepository subscriptionRepository;

    @Cacheable(value = CacheNames.FEED_CACHE, key = "#currentUserId")
    @Override
    public FeedPageResponse getMyFeed(Long currentUserId, FeedSearchRequest searchRequest) {
        PageRequest pageRequest = feedMapper.buildPageRequest(searchRequest);
        Page<Feed> feedsPage = feedRepository.findAllByUserId(currentUserId, pageRequest);
        List<FeedDto> feedsForUser = feedMapper.toListDtos(feedsPage.getContent());

        log.info("Fetched {} feeds for userId={}", feedsForUser.size(), currentUserId);
        return feedMapper.buildFeedPageResponse(feedsForUser, feedsPage);
    }

    @Transactional
    @Override
    public void distributePostToSubscribers(PostCreatedEvent event) {
        List<Subscription> subscriptions = subscriptionRepository.findAllByFolloweeId(event.getAuthorId());
        List<Feed> buffer = new ArrayList<>();
        Set<Long> userIdsToEvictCache = new HashSet<>();

        for(Subscription subscription : subscriptions) {
            buffer.add(Feed.builder()
                    .postId(event.getPostId())
                    .userId(subscription.getFollowerId())
                    .createdAt(event.getCreatedAt())
                    .build());

            userIdsToEvictCache.add(subscription.getFollowerId());

            if(buffer.size() == 50) {
                safeSaveFeeds(buffer);
                buffer.clear();
            }
        }

        if(!userIdsToEvictCache.isEmpty()) {
            feedCacheInvalidator.evictFeedCache(userIdsToEvictCache);
        }

        if(!buffer.isEmpty()) {
            safeSaveFeeds(buffer);
        }
    }

    @Transactional
    @Override
    public void deleteFeedsByPostId(PostRemovedEvent event) {
        int deleted = feedRepository.deleteAllByPostId(event.getPostId());
        log.info("Deleted {} feeds by postId={}", deleted, event.getPostId());
    }

    private void safeSaveFeeds(List<Feed> feedsToSave) {
        try {
            feedRepository.saveAll(feedsToSave);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicated feed entry detected, skipping duplicates: {}", e.getMessage());
        }
    }
}
