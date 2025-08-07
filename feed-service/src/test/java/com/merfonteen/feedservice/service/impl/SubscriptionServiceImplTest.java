package com.merfonteen.feedservice.service.impl;

import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.feedservice.client.UserClient;
import com.merfonteen.feedservice.dto.SubscriptionDto;
import com.merfonteen.feedservice.mapper.SubscriptionMapper;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.repository.SubscriptionRepository;
import com.merfonteen.feedservice.service.OutboxService;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.merfonteen.feedservice.service.impl.SubscriptionServiceImplTest.TestResources.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private UserClient userClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    void testGetMySubscriptions_Success() {
        Subscription sub1 = buildSubscription(FIRST_SUBSCRIPTION_ID, USER_ID, FIRST_FOLLOWEE_ID);
        Subscription sub2 = buildSubscription(SECOND_SUBSCRIPTION_ID, USER_ID, SECOND_FOLLOWEE_ID);

        List<SubscriptionDto> subscriptionDtos = buildSubscriptionDtoList(List.of(sub1, sub2));

        when(subscriptionRepository.findAllByFollowerId(USER_ID)).thenReturn(List.of(sub1, sub2));
        when(subscriptionMapper.toDtos(List.of(sub1, sub2))).thenReturn(subscriptionDtos);

        List<SubscriptionDto> result = subscriptionService.getMySubscriptions(USER_ID);

        assertThat(result).isEqualTo(subscriptionDtos);
        verify(subscriptionRepository, times(1)).findAllByFollowerId(USER_ID);
    }

    @Test
    void testGetMySubscribers_Success() {
        Subscription sub1 = buildSubscription(FIRST_SUBSCRIPTION_ID, FIRST_FOLLOWER_ID, USER_ID);
        Subscription sub2 = buildSubscription(SECOND_SUBSCRIPTION_ID, SECOND_FOLLOWER_ID, USER_ID);

        List<SubscriptionDto> subscriptionDtos = buildSubscriptionDtoList(List.of(sub1, sub2));

        when(subscriptionRepository.findAllByFolloweeId(USER_ID)).thenReturn(List.of(sub1, sub2));
        when(subscriptionMapper.toDtos(List.of(sub1, sub2))).thenReturn(subscriptionDtos);

        List<SubscriptionDto> result = subscriptionService.getMySubscribers(USER_ID);

        assertThat(result).isEqualTo(subscriptionDtos);
        verify(subscriptionRepository, times(1)).findAllByFolloweeId(USER_ID);
    }

    @Test
    void testFollow_Success() {
        Subscription subscription = buildSubscription(FIRST_SUBSCRIPTION_ID, FIRST_FOLLOWER_ID, FIRST_FOLLOWEE_ID);
        SubscriptionDto expectedDto = buildSubscriptionDto(subscription);

        doNothing().when(userClient).checkUserExists(FIRST_FOLLOWEE_ID);

        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionMapper.toDto(any(Subscription.class))).thenReturn(expectedDto);

        SubscriptionDto result = subscriptionService.follow(FIRST_FOLLOWEE_ID, FIRST_FOLLOWER_ID);

        assertEquals(expectedDto, result);
        verify(userClient).checkUserExists(FIRST_FOLLOWEE_ID);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(subscriptionMapper).toDto(any(Subscription.class));
    }

    @Test
    void testFollow_FollowingYourself_ShouldThrowException() {
        doNothing().when(userClient).checkUserExists(USER_ID);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> subscriptionService.follow(USER_ID, USER_ID));

        assertEquals("Cannot follow yourself", exception.getMessage());
        verifyNoInteractions(subscriptionRepository, subscriptionMapper);
    }

    @Test
    void testFollow_WhenUserDoesNotExists_ShouldThrowException() {
        FeignException.NotFound feignNotFound = buildUserNotFoundException();

        doThrow(feignNotFound).when(userClient).checkUserExists(FIRST_FOLLOWEE_ID);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> subscriptionService.follow(FIRST_FOLLOWEE_ID, FIRST_FOLLOWER_ID)
        );

        assertEquals("User with id '%d' not found".formatted(FIRST_FOLLOWEE_ID), exception.getMessage());

        verify(userClient).checkUserExists(FIRST_FOLLOWEE_ID);
        verifyNoMoreInteractions(subscriptionRepository, subscriptionMapper);
    }

    @Test
    void testUnfollow_Success() {
        Subscription subscription = buildSubscription(FIRST_SUBSCRIPTION_ID, FIRST_FOLLOWER_ID, FIRST_FOLLOWEE_ID);
        SubscriptionDto subscriptionDto = buildSubscriptionDto(subscription);

        doNothing().when(userClient).checkUserExists(FIRST_FOLLOWEE_ID);
        when(subscriptionRepository.findSubscriptionByFollowerIdAndFolloweeId(FIRST_FOLLOWER_ID, FIRST_FOLLOWEE_ID))
                .thenReturn(Optional.of(subscription));
        doNothing().when(subscriptionRepository).delete(subscription);
        when(subscriptionMapper.toDto(any(Subscription.class))).thenReturn(subscriptionDto);

        SubscriptionDto result = subscriptionService.unfollow(FIRST_FOLLOWEE_ID, FIRST_FOLLOWER_ID);

        assertEquals(subscriptionDto, result);

        verify(userClient).checkUserExists(FIRST_FOLLOWEE_ID);
        verify(subscriptionRepository).findSubscriptionByFollowerIdAndFolloweeId(FIRST_FOLLOWER_ID, FIRST_FOLLOWEE_ID);
        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    void testUnfollow_whenSubscriptionDoesNotExist_shouldThrowNotFoundException() {
        doNothing().when(userClient).checkUserExists(FIRST_FOLLOWEE_ID);
        when(subscriptionRepository.findSubscriptionByFollowerIdAndFolloweeId(FIRST_FOLLOWER_ID, FIRST_FOLLOWEE_ID))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> subscriptionService.unfollow(FIRST_FOLLOWEE_ID, FIRST_FOLLOWER_ID));

        assertEquals("Subscription was not found with follower id '%d' and followee id '%d'"
                .formatted(FIRST_FOLLOWER_ID, FIRST_FOLLOWEE_ID), exception.getMessage());

        verify(userClient).checkUserExists(FIRST_FOLLOWEE_ID);
        verify(subscriptionRepository).findSubscriptionByFollowerIdAndFolloweeId(FIRST_FOLLOWER_ID, FIRST_FOLLOWEE_ID);
        verifyNoMoreInteractions(subscriptionRepository, subscriptionMapper);
    }

    static class TestResources {
        static final Long USER_ID = 1L;
        static final Long FIRST_SUBSCRIPTION_ID = 111L;
        static final Long SECOND_SUBSCRIPTION_ID = 222L;
        static final Long FIRST_FOLLOWER_ID = 12L;
        static final Long SECOND_FOLLOWER_ID = 31L;
        static final Long FIRST_FOLLOWEE_ID = 22L;
        static final Long SECOND_FOLLOWEE_ID = 20L;

        static List<SubscriptionDto> buildSubscriptionDtoList(List<Subscription> subscriptions) {
            return subscriptions.stream()
                    .map(TestResources::buildSubscriptionDto)
                    .toList();
        }

        static Subscription buildSubscription(Long id, Long followerId, Long followeeId) {
            return Subscription.builder()
                    .id(id)
                    .followerId(followerId)
                    .followeeId(followeeId)
                    .createdAt(Instant.now())
                    .build();
        }

        static SubscriptionDto buildSubscriptionDto(Subscription subscription) {
            return SubscriptionDto.builder()
                    .id(subscription.getId())
                    .followerId(subscription.getFollowerId())
                    .followeeId(subscription.getFolloweeId())
                    .createdAt(subscription.getCreatedAt())
                    .build();
        }

        static FeignException.NotFound buildUserNotFoundException() {
            return (FeignException.NotFound) FeignException.errorStatus("checkUserExists",
                    Response.builder()
                            .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, Charset.defaultCharset(), null))
                            .status(404)
                            .reason("Not Found")
                            .headers(Map.of())
                            .build()
            );
        }
    }
}