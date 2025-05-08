package com.merfonteen.feedservice.service.impl;

import com.merfonteen.feedservice.dto.FeedDto;
import com.merfonteen.feedservice.dto.FeedPageResponseDto;
import com.merfonteen.feedservice.dto.PostCreatedEvent;
import com.merfonteen.feedservice.mapper.FeedMapper;
import com.merfonteen.feedservice.model.Feed;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.repository.FeedRepository;
import com.merfonteen.feedservice.repository.SubscriptionRepository;
import com.merfonteen.feedservice.service.FeedCacheService;
import com.merfonteen.feedservice.service.FeedService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final FeedCacheService feedCacheService;
    private final SubscriptionRepository subscriptionRepository;

    @Cacheable(value = "feed", key = "#currentUserId + ':' + #page + ':' + #size")
    @Override
    public FeedPageResponseDto getMyFeed(Long currentUserId, int page, int size) {
        if(size > 100) {
            size = 100;
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"));
        Page<Feed> feedsPage = feedRepository.findAllByUserId(currentUserId, pageRequest);
        List<FeedDto> feedsForUser = feedMapper.toListDtos(feedsPage.getContent());

        return FeedPageResponseDto.builder()
                .feeds(feedsForUser)
                .currentPage(feedsPage.getNumber())
                .totalElements(feedsPage.getTotalElements())
                .totalPages(feedsPage.getTotalPages())
                .isLastPage(feedsPage.isLast())
                .build();
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
            feedCacheService.evictFeedCache(userIdsToEvictCache);
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
