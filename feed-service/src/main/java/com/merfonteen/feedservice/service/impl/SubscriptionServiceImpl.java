package com.merfonteen.feedservice.service.impl;

import com.merfonteen.feedservice.client.UserClient;
import com.merfonteen.feedservice.dto.SubscriptionDto;
import com.merfonteen.feedservice.dto.event.SubscriptionCreatedEvent;
import com.merfonteen.feedservice.exception.BadRequestException;
import com.merfonteen.feedservice.exception.NotFoundException;
import com.merfonteen.feedservice.kafka.eventProducer.SubscriptionEventProducer;
import com.merfonteen.feedservice.mapper.SubscriptionMapper;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.repository.SubscriptionRepository;
import com.merfonteen.feedservice.service.SubscriptionService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Primary
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserClient userClient;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEventProducer subscriptionEventProducer;

    @Override
    public List<SubscriptionDto> getMySubscriptions(Long currentUserId) {
        List<Subscription> subscriptions = subscriptionRepository.findAllByFollowerId(currentUserId);
        log.info("Getting all subscriptions for user with id: {}", currentUserId);
        return subscriptions.stream()
                .map(subscriptionMapper::toDto)
                .toList();
    }

    @Override
    public List<SubscriptionDto> getMySubscribers(Long currentUserId) {
        List<Subscription> subscribers = subscriptionRepository.findAllByFolloweeId(currentUserId);
        log.info("Getting all subscribers for user with id: {}", currentUserId);
        return subscribers.stream()
                .map(subscriptionMapper::toDto)
                .toList();
    }

    @Override
    public List<SubscriptionDto> getUserSubscribersByUserId(Long userId) {
        checkUserExistsOrThrowException(userId);
        List<Subscription> userSubscribers = subscriptionRepository.findAllByFolloweeId(userId);
        log.info("Getting all user's subscribers for user with id: {}", userId);
        return subscriptionMapper.toDtos(userSubscribers);
    }

    @Transactional
    @Override
    public SubscriptionDto follow(Long targetUserId, Long currentUserId) {
        checkUserExistsOrThrowException(targetUserId);

        if (Objects.equals(currentUserId, targetUserId)) {
            throw new BadRequestException("Cannot follow yourself");
        }

        Subscription subscription = Subscription.builder()
                .followerId(currentUserId)
                .followeeId(targetUserId)
                .createdAt(Instant.now())
                .build();

        subscriptionRepository.save(subscription);
        log.info("Subscription was successfully created with follower id '{}' and followee id '{}'",
                currentUserId, targetUserId);

        subscriptionEventProducer.sendSubscriptionCreatedEvent(new SubscriptionCreatedEvent(
                        subscription.getId(), currentUserId, targetUserId));

        return subscriptionMapper.toDto(subscription);
    }

    @Transactional
    @Override
    public SubscriptionDto unfollow(Long targetUserId, Long currentUserId) {
        checkUserExistsOrThrowException(targetUserId);
        Subscription subscriptionToDelete = findSubscriptionByFollowerIdAndFolloweeIdOrThrowException(targetUserId, currentUserId);
        subscriptionRepository.delete(subscriptionToDelete);
        log.info("Subscription was successfully deleted: {}", subscriptionToDelete);
        return subscriptionMapper.toDto(subscriptionToDelete);
    }

    private Subscription findSubscriptionByFollowerIdAndFolloweeIdOrThrowException(Long targetUserId, Long currentUserId) {
        return subscriptionRepository
                .findSubscriptionByFollowerIdAndFolloweeId(currentUserId, targetUserId)
                .orElseThrow(() -> new NotFoundException(
                                String.format(
                                        "Subscription was not found with follower id '%d' and followee id '%d'",
                                        currentUserId, targetUserId)
                        )
                );
    }

    private void checkUserExistsOrThrowException(Long targetUserId) {
        try {
            userClient.checkUserExists(targetUserId);
        } catch (FeignException.NotFound e) {
            log.error("User with id '{}' not found", targetUserId);
            throw new NotFoundException(String.format("User with id '%d' not found", targetUserId));
        }
    }
}
