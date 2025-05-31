package com.merfonteen.feedservice.service.impl;

import com.merfonteen.feedservice.dto.FeedDto;
import com.merfonteen.feedservice.dto.FeedPageResponseDto;
import com.merfonteen.feedservice.mapper.FeedMapper;
import com.merfonteen.feedservice.model.Feed;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.repository.FeedRepository;
import com.merfonteen.feedservice.repository.SubscriptionRepository;
import com.merfonteen.feedservice.service.FeedCacheService;
import com.merfonteen.kafkaEvents.PostCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class FeedServiceImplTest {

    @Mock
    private FeedMapper feedMapper;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedCacheService feedCacheService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private FeedServiceImpl feedService;

    @Test
    void testGetMyFeed_Success() {
        Long userId = 1L;
        int page = 0;
        int size = 5;

        List<Feed> feeds = List.of(
                Feed.builder().id(1L).userId(userId).postId(101L).createdAt(Instant.now()).build(),
                Feed.builder().id(2L).userId(userId).postId(102L).createdAt(Instant.now()).build()
        );

        Page<Feed> feedPage = new PageImpl<>(feeds, PageRequest.of(page, size), feeds.size());

        List<FeedDto> feedDtos = List.of(
                new FeedDto(1L, userId, 101L, Instant.now()),
                new FeedDto(2L, userId, 102L, Instant.now())
        );

        when(feedRepository.findAllByUserId(eq(userId), any(PageRequest.class))).thenReturn(feedPage);
        when(feedMapper.toListDtos(feeds)).thenReturn(feedDtos);

        FeedPageResponseDto result = feedService.getMyFeed(userId, page, size);


        assertEquals(feedDtos.size(), result.getFeeds().size());
        assertEquals(page, result.getCurrentPage());
        assertEquals(feeds.size(), result.getTotalElements());
        assertEquals(1, result.getTotalPages());
    }

    @Test
    void testDistributePostToSubscribers_ShouldDistributeInBatchesAndEvictCache() {
        long authorId = 1L;
        long postId = 123L;
        Instant createdAt = Instant.now();

        List<Subscription> subscriptions = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            subscriptions.add(
                    Subscription.builder()
                            .followeeId(authorId)
                            .followerId(i)
                            .createdAt(createdAt)
                            .build()
            );
        }

        when(subscriptionRepository.findAllByFolloweeId(authorId)).thenReturn(subscriptions);

        PostCreatedEvent event = PostCreatedEvent.builder()
                .authorId(authorId)
                .postId(postId)
                .createdAt(createdAt)
                .build();

        feedService.distributePostToSubscribers(event);

        verify(feedRepository, times(2)).saveAll(anyList());

        ArgumentCaptor<Set<Long>> cacheEvictCaptor = ArgumentCaptor.forClass(Set.class);
        verify(feedCacheService, times(1)).evictFeedCache(cacheEvictCaptor.capture());

        Set<Long> evictedUserIds = cacheEvictCaptor.getValue();
        assertEquals(100, evictedUserIds.size());
    }
}