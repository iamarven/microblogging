package com.merfonteen.feedservice.service.impl;

import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.feedservice.client.UserClient;
import com.merfonteen.feedservice.dto.SubscriptionDto;
import com.merfonteen.feedservice.kafka.eventProducer.SubscriptionEventProducer;
import com.merfonteen.feedservice.mapper.SubscriptionMapper;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.repository.SubscriptionRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private UserClient userClient;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionEventProducer subscriptionEventProducer;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    void testGetMySubscriptions_Success() {
        Long userId = 1L;
        Subscription subscription1 = Subscription.builder()
                .id(1L)
                .followerId(userId)
                .followeeId(2L)
                .createdAt(Instant.now())
                .build();

        Subscription subscription2 = Subscription.builder()
                .id(2L)
                .followerId(userId)
                .followeeId(3L)
                .createdAt(Instant.now())
                .build();

        SubscriptionDto dto1 = new SubscriptionDto(1L, userId, 2L, subscription1.getCreatedAt());
        SubscriptionDto dto2 = new SubscriptionDto(2L, userId, 3L, subscription2.getCreatedAt());

        when(subscriptionRepository.findAllByFollowerId(userId))
                .thenReturn(List.of(subscription1, subscription2));
        when(subscriptionMapper.toDto(subscription1)).thenReturn(dto1);
        when(subscriptionMapper.toDto(subscription2)).thenReturn(dto2);

        List<SubscriptionDto> result = subscriptionService.getMySubscriptions(userId);

        assertEquals(List.of(dto1, dto2), result);
        verify(subscriptionRepository, times(1)).findAllByFollowerId(userId);
        verify(subscriptionMapper, times(1)).toDto(subscription1);
        verify(subscriptionMapper, times(1)).toDto(subscription2);
    }

    @Test
    void testGetMySubscribers_Success() {
        Long userId = 10L;
        Subscription sub1 = Subscription.builder()
                .id(100L)
                .followerId(2L)
                .followeeId(userId)
                .createdAt(Instant.now())
                .build();

        Subscription sub2 = Subscription.builder()
                .id(101L)
                .followerId(3L)
                .followeeId(userId)
                .createdAt(Instant.now())
                .build();

        SubscriptionDto dto1 = new SubscriptionDto(100L, 2L, userId, sub1.getCreatedAt());
        SubscriptionDto dto2 = new SubscriptionDto(101L, 3L, userId, sub2.getCreatedAt());

        when(subscriptionRepository.findAllByFolloweeId(userId)).thenReturn(List.of(sub1, sub2));
        when(subscriptionMapper.toDto(sub1)).thenReturn(dto1);
        when(subscriptionMapper.toDto(sub2)).thenReturn(dto2);

        List<SubscriptionDto> result = subscriptionService.getMySubscribers(userId);

        assertEquals(List.of(dto1, dto2), result);
        verify(subscriptionRepository, times(1)).findAllByFolloweeId(userId);
        verify(subscriptionMapper, times(1)).toDto(sub1);
        verify(subscriptionMapper, times(1)).toDto(sub2);
    }

    @Test
    void testFollow_Success() {
        Long followerId = 1L;
        Long followeeId = 2L;

        Subscription subscription = Subscription.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .createdAt(Instant.now())
                .build();

        SubscriptionDto expectedDto = new SubscriptionDto(null, followerId, followeeId, subscription.getCreatedAt());

        doNothing().when(userClient).checkUserExists(followeeId);

        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);
        when(subscriptionMapper.toDto(any(Subscription.class))).thenReturn(expectedDto);

        SubscriptionDto result = subscriptionService.follow(followeeId, followerId);

        assertEquals(expectedDto, result);
        verify(userClient).checkUserExists(followeeId);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(subscriptionMapper).toDto(any(Subscription.class));
    }

    @Test
    void testFollow_FollowingYourself_ShouldThrowException() {
        Long userId = 5L;

        doNothing().when(userClient).checkUserExists(userId);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> subscriptionService.follow(userId, userId));

        assertEquals("Cannot follow yourself", exception.getMessage());
        verifyNoInteractions(subscriptionRepository, subscriptionMapper);
    }

    @Test
    void testFollow_WhenUserDoesNotExists_ShouldThrowException() {
        Long followerId = 1L;
        Long followeeId = 2L;

        FeignException.NotFound feignNotFound =
                (FeignException.NotFound) FeignException.errorStatus("checkUserExists",
                        Response.builder()
                                .request(Request.create(Request.HttpMethod.GET, "", Map.of(), null, Charset.defaultCharset(), null))
                                .status(404)
                                .reason("Not Found")
                                .headers(Map.of())
                                .build()
                );

        doThrow(feignNotFound).when(userClient).checkUserExists(followeeId);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> subscriptionService.follow(followeeId, followerId)
        );

        assertEquals("User with id '2' not found", exception.getMessage());

        verify(userClient).checkUserExists(followeeId);
        verifyNoMoreInteractions(subscriptionRepository, subscriptionMapper);
    }

    @Test
    void testUnfollow_Success() {
        Long followerId = 1L;
        Long followeeId = 2L;

        Subscription subscription = Subscription.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .createdAt(Instant.now())
                .build();

        SubscriptionDto subscriptionDto = SubscriptionDto.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .build();

        doNothing().when(userClient).checkUserExists(followeeId);
        when(subscriptionRepository.findSubscriptionByFollowerIdAndFolloweeId(followerId, followeeId))
                .thenReturn(Optional.of(subscription));
        doNothing().when(subscriptionRepository).delete(subscription);
        when(subscriptionMapper.toDto(any(Subscription.class))).thenReturn(subscriptionDto);

        SubscriptionDto result = subscriptionService.unfollow(followeeId, followerId);

        assertEquals(subscriptionDto, result);

        verify(userClient).checkUserExists(followeeId);
        verify(subscriptionRepository).findSubscriptionByFollowerIdAndFolloweeId(followerId, followeeId);
        verify(subscriptionRepository).delete(subscription);
        verify(subscriptionMapper).toDto(any(Subscription.class));
    }

    @Test
    void unfollow_shouldThrowNotFoundException_whenSubscriptionDoesNotExist() {
        Long followerId = 1L;
        Long followeeId = 2L;

        doNothing().when(userClient).checkUserExists(followeeId);
        when(subscriptionRepository.findSubscriptionByFollowerIdAndFolloweeId(followerId, followeeId))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> subscriptionService.unfollow(followeeId, followerId));

        assertEquals("Subscription was not found with follower id '1' and followee id '2'", exception.getMessage());

        verify(userClient).checkUserExists(followeeId);
        verify(subscriptionRepository).findSubscriptionByFollowerIdAndFolloweeId(followerId, followeeId);
        verifyNoMoreInteractions(subscriptionRepository, subscriptionMapper);
    }

}