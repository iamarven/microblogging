package com.merfonteen.feedservice.service.impl;

import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.feedservice.client.UserClient;
import com.merfonteen.feedservice.config.CacheNames;
import com.merfonteen.feedservice.dto.SubscriptionDto;
import com.merfonteen.feedservice.kafka.eventProducer.SubscriptionEventProducer;
import com.merfonteen.feedservice.mapper.SubscriptionMapper;
import com.merfonteen.feedservice.model.Subscription;
import com.merfonteen.feedservice.repository.SubscriptionRepository;
import com.merfonteen.feedservice.service.SubscriptionService;
import com.merfonteen.kafkaEvents.SubscriptionCreatedEvent;
import com.merfonteen.kafkaEvents.SubscriptionRemovedEvent;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    @Cacheable(value = CacheNames.SUBSCRIPTION_CACHE, key = "#currentUserId")
    @Override
    public List<SubscriptionDto> getMySubscriptions(Long currentUserId) {
        List<Subscription> subscriptions = subscriptionRepository.findAllByFollowerId(currentUserId);
        log.info("Getting all subscriptions for user with id: {}", currentUserId);
        return subscriptionMapper.toDtos(subscriptions);
    }

    @Cacheable(value = CacheNames.SUBSCRIBERS_CACHE, key = "#currentUserId")
    @Override
    public List<SubscriptionDto> getMySubscribers(Long currentUserId) {
        List<Subscription> subscribers = subscriptionRepository.findAllByFolloweeId(currentUserId);
        log.info("Getting all subscribers for user with id: {}", currentUserId);
        return subscriptionMapper.toDtos(subscribers);
    }

    @Cacheable(value = CacheNames.SUBSCRIBERS_CACHE, key = "#userId")
    @Override
    public List<SubscriptionDto> getUserSubscribersByUserId(Long userId) {
        checkUserExistsOrThrowException(userId);
        List<Subscription> userSubscribers = subscriptionRepository.findAllByFolloweeId(userId);
        log.info("Getting all user's subscribers for user with id: {}", userId);
        return subscriptionMapper.toDtos(userSubscribers);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.SUBSCRIPTION_CACHE, key = "#currentUserId"),
            @CacheEvict(value = CacheNames.SUBSCRIBERS_CACHE, key = "#targetUserId")
    })
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

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription was successfully created with follower id '{}' and followee id '{}'",
                currentUserId, targetUserId);

        subscriptionEventProducer.sendSubscriptionCreatedEvent(new SubscriptionCreatedEvent(
                        saved.getId(), currentUserId, targetUserId));

        return subscriptionMapper.toDto(saved);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheNames.SUBSCRIPTION_CACHE, key = "#currentUserId"),
            @CacheEvict(value = CacheNames.SUBSCRIBERS_CACHE, key = "#targetUserId")
    })
    @Transactional
    @Override
    public SubscriptionDto unfollow(Long targetUserId, Long currentUserId) {
        checkUserExistsOrThrowException(targetUserId);
        Subscription subscriptionToDelete = getSubscriptionByFollowerIdAndFolloweeIdOrThrowException(targetUserId, currentUserId);

        subscriptionRepository.delete(subscriptionToDelete);
        log.info("Subscription was successfully deleted: {}", subscriptionToDelete);

        subscriptionEventProducer.sendSubscriptionRemovedEvent(new SubscriptionRemovedEvent(targetUserId));

        return subscriptionMapper.toDto(subscriptionToDelete);
    }

    private Subscription getSubscriptionByFollowerIdAndFolloweeIdOrThrowException(Long targetUserId, Long currentUserId) {
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
