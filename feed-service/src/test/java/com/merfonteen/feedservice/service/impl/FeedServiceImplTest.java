package com.merfonteen.feedservice.service.impl;

import com.merfonteen.feedservice.dto.FeedDto;
import com.merfonteen.feedservice.dto.FeedPageResponse;
import com.merfonteen.feedservice.dto.FeedSearchRequest;
import com.merfonteen.feedservice.mapper.FeedMapper;
import com.merfonteen.feedservice.model.Feed;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.repository.FeedRepository;
import com.merfonteen.feedservice.repository.SubscriptionRepository;
import com.merfonteen.feedservice.service.FeedCacheInvalidator;
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
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.merfonteen.feedservice.service.impl.FeedServiceImplTest.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceImplTest {

    @Mock
    private FeedMapper feedMapper;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedCacheInvalidator feedCacheInvalidator;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private FeedServiceImpl feedService;

    @Test
    void testGetMyFeed_Success() {
        List<Feed> feeds = buildFeeds();
        Page<Feed> feedPage = buildFeedPage(feeds);
        List<FeedDto> feedDtos = buildFeedDtos();

        when(feedMapper.buildPageRequest(buildFeedSearchRequest())).thenReturn(buildPageRequest());
        when(feedRepository.findAllByUserId(USER_ID, buildPageRequest())).thenReturn(feedPage);
        when(feedMapper.toListDtos(feeds)).thenReturn(feedDtos);
        when(feedMapper.buildFeedPageResponse(feedDtos, feedPage)).thenReturn(buildFeedPageResponse(feedDtos, feedPage));

        FeedPageResponse result = feedService.getMyFeed(USER_ID, buildFeedSearchRequest());

        assertThat(result.getFeeds()).isEqualTo(feedDtos);
    }

    @Test
    void testDistributePostToSubscribers_ShouldDistributeInBatchesAndEvictCache() {
        PostCreatedEvent event = buildPostCreatedEvent();
        List<Subscription> subscriptions = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            subscriptions.add(
                    Subscription.builder()
                            .followeeId(AUTHOR_ID)
                            .followerId(i)
                            .createdAt(CREATED_AT)
                            .build()
            );
        }

        when(subscriptionRepository.findAllByFolloweeId(AUTHOR_ID)).thenReturn(subscriptions);

        feedService.distributePostToSubscribers(event);

        verify(feedRepository, times(2)).saveAll(anyList());
        verify(feedCacheInvalidator, times(1)).evictFeedCache(any(Set.class));
    }

    static class TestResources {
        static final Long AUTHOR_ID = 100L;
        static final Long USER_ID = 1L;
        static final Long FIRST_POST_ID = 50L;
        static final Long SECOND_POST_ID = 70L;
        static final int PAGE = 0;
        static final int SIZE = 10;
        static final Instant CREATED_AT = Instant.parse(Instant.now().toString());

        static List<Feed> buildFeeds() {
            return List.of(
                    Feed.builder().id(1L).userId(USER_ID).postId(FIRST_POST_ID).createdAt(CREATED_AT).build(),
                    Feed.builder().id(2L).userId(USER_ID).postId(SECOND_POST_ID).createdAt(CREATED_AT).build()
            );
        }

        static FeedPageResponse buildFeedPageResponse(List<FeedDto> feedDtos, Page<Feed> feedPage) {
            return FeedPageResponse.builder()
                    .feeds(feedDtos)
                    .currentPage(feedPage.getNumber())
                    .totalElements(feedPage.getTotalElements())
                    .totalPages(feedPage.getTotalPages())
                    .isLastPage(feedPage.isLast())
                    .build();
        }

        static Page<Feed> buildFeedPage(List<Feed> feeds) {
            return  new PageImpl<>(feeds, PageRequest.of(PAGE, SIZE), feeds.size());
        }

        static List<FeedDto> buildFeedDtos() {
            return List.of(
                    new FeedDto(1L, USER_ID, FIRST_POST_ID, Instant.now()),
                    new FeedDto(2L, USER_ID, SECOND_POST_ID, Instant.now())
            );
        }

        static FeedSearchRequest buildFeedSearchRequest() {
            return FeedSearchRequest.builder()
                    .page(PAGE)
                    .size(SIZE)
                    .build();
        }

        static PostCreatedEvent buildPostCreatedEvent() {
            return PostCreatedEvent.builder()
                    .authorId(AUTHOR_ID)
                    .postId(FIRST_POST_ID)
                    .createdAt(CREATED_AT)
                    .build();
        }

        static PageRequest buildPageRequest() {
            return PageRequest.of(PAGE, SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }
}