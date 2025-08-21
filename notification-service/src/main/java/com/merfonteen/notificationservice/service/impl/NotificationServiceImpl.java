package com.merfonteen.notificationservice.service.impl;

import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.notificationservice.client.FeedClient;
import com.merfonteen.notificationservice.client.PostClient;
import com.merfonteen.notificationservice.dto.NotificationResponse;
import com.merfonteen.notificationservice.dto.NotificationsPageResponse;
import com.merfonteen.notificationservice.dto.NotificationsSearchRequest;
import com.merfonteen.notificationservice.dto.SubscriptionDto;
import com.merfonteen.notificationservice.mapper.NotificationMapper;
import com.merfonteen.notificationservice.model.Notification;
import com.merfonteen.notificationservice.model.enums.NotificationFilter;
import com.merfonteen.notificationservice.model.enums.NotificationType;
import com.merfonteen.notificationservice.repository.NotificationRepository;
import com.merfonteen.notificationservice.service.NotificationService;
import com.merfonteen.notificationservice.service.redis.RedisCounter;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationServiceImpl implements NotificationService {
    private final PostClient postClient;
    private final FeedClient feedClient;
    private final RedisCounter redisCounter;
    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

    @Transactional
    @Override
    public NotificationsPageResponse getMyNotifications(Long currentUserId, NotificationsSearchRequest searchRequest) {
        NotificationFilter filter = NotificationFilter.from(searchRequest.getFilterRaw());

        PageRequest request = notificationMapper.buildPageRequest(searchRequest.getPage(), searchRequest.getSize());
        Page<Notification> userNotifications;

        switch (filter) {
            case READ -> userNotifications =
                    notificationRepository.findAllByReceiverIdAndIsReadTrue(currentUserId, request);
            case UNREAD -> userNotifications =
                    notificationRepository.findAllByReceiverIdAndIsReadFalse(currentUserId, request);
            default -> userNotifications =
                    notificationRepository.findAllByReceiverId(currentUserId, request);
        }

        List<NotificationResponse> notificationResponses = notificationMapper.toDtos(userNotifications.getContent());
        log.info("User '{}' fetched {} notifications (page={}, size={})",
                currentUserId, notificationResponses.size(),
                searchRequest.getPage(), searchRequest.getSize()
        );

        if (filter.equals(NotificationFilter.UNREAD) || filter.equals(NotificationFilter.ALL)) {
            for (Notification notification : userNotifications) {
                notification.setIsRead(true);
            }
            redisCounter.refreshCountUnreadNotifications(currentUserId);
        }

        notificationRepository.saveAll(userNotifications);

        return notificationMapper.buildNotificationsPageResponse(notificationResponses, userNotifications);
    }

    @Override
    public Long countUnreadNotifications(Long currentUserId) {
        String cacheValue = redisCounter.getCachedValue(currentUserId);

        if (cacheValue != null) {
            return Long.parseLong(cacheValue);
        }

        Long countFromDb = notificationRepository.countAllByReceiverIdAndIsReadFalse(currentUserId);
        redisCounter.setCounter(currentUserId, countFromDb);

        return countFromDb;
    }

    @Transactional
    @Override
    public NotificationResponse markAsRead(Long notificationId, Long currentUserId) {
        Notification notification = getNotificationByIdAndReceiverId(notificationId, currentUserId);
        if (notification.getIsRead().equals(Boolean.FALSE)) {
            notification.setIsRead(true);
            Notification saved = notificationRepository.save(notification);
            redisCounter.decrementCounter(currentUserId);
            return notificationMapper.toDto(saved);

        }
        return notificationMapper.toDto(notification);
    }

    @Transactional
    @Override
    public void deleteNotification(Long id, Long currentUserId) {
        Notification notificationToDelete = notificationRepository.findById(id).orElse(null);

        if (notificationToDelete == null) {
            return;
        }

        if (!Objects.equals(notificationToDelete.getReceiverId(), currentUserId)) {
            throw new BadRequestException("You cannot delete not your own notifications");
        }

        notificationRepository.delete(notificationToDelete);
        log.info("Deleted a notification '{}' by user '{}'", id, currentUserId);

        redisCounter.decrementCounter(currentUserId);
    }

    @Transactional
    @Override
    public void deleteNotificationsForEntity(Long entityId, NotificationType type) {
        List<Notification> notificationsToDelete = notificationRepository.findByEntityIdAndType(entityId, type);
        if (notificationsToDelete.isEmpty()) return;

        Map<Long, List<Notification>> byReceiver =
                notificationsToDelete.stream().collect(Collectors.groupingBy(Notification::getReceiverId));

        notificationRepository.deleteAll(notificationsToDelete);
        log.info("Deleted {} notifications by receiver '{}'", notificationsToDelete.size(), entityId);

        byReceiver.keySet().forEach(redisCounter::refreshCountUnreadNotifications);
    }

    @Transactional
    @Override
    public void sendLikeNotification(Long senderId, Long likeId, Long postId) {
        Long postAuthorId = postClient.getPostAuthorId(postId);
        Notification notification = notificationMapper.buildNotification(
                senderId, postAuthorId, likeId, NotificationType.LIKE);

        redisCounter.incrementCounter(postAuthorId);

        notificationRepository.save(notification);
    }

    @Transactional
    @Override
    public void sendPostNotification(Long postId, Long authorId) {
        List<SubscriptionDto> userSubscribers = getUserSubscribers(authorId);
        List<Notification> buffer = new ArrayList<>();

        if (!userSubscribers.isEmpty()) {
            for (SubscriptionDto subscription : userSubscribers) {

                Notification notification = notificationMapper.buildNotification(
                        authorId, subscription.getFollowerId(), postId, NotificationType.POST);

                buffer.add(notification);
                redisCounter.incrementCounter(subscription.getFollowerId());

                if (buffer.size() == 50) {
                    safeSaveNotifications(buffer);
                    buffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                safeSaveNotifications(buffer);
            }
        }
    }

    @Transactional
    @Override
    public void sendFollowNotification(Long followerId, Long followeeId, Long subscriptionId) {
        Notification notification = notificationMapper.buildNotification(
                followerId, followeeId, subscriptionId, NotificationType.SUBSCRIPTION);

        redisCounter.incrementCounter(followeeId);
        notificationRepository.save(notification);
    }

    @Override
    public void sendCommentNotification(Long commentId, Long postId, Long leftCommentUserId) {
        Long postAuthorId = postClient.getPostAuthorId(postId);

        Notification notification = notificationMapper.buildNotification(
                leftCommentUserId, postAuthorId, commentId, NotificationType.COMMENT);

        redisCounter.incrementCounter(postAuthorId);
        notificationRepository.save(notification);
    }

    private Notification getNotificationByIdAndReceiverId(Long id, Long currentUserId) {
        return notificationRepository.findByIdAndReceiverId(id, currentUserId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Notification with id '%d' and receiverId '%d 'not found", id, currentUserId)));
    }

    private void safeSaveNotifications(List<Notification> notificationsToSave) {
        try {
            notificationRepository.saveAll(notificationsToSave);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicated notification entry detected, skipping duplicates: {}", e.getMessage());
        }
    }

    private List<SubscriptionDto> getUserSubscribers(Long authorId) {
        List<SubscriptionDto> userSubscribers = Collections.emptyList();
        try {
            userSubscribers = feedClient.getUserSubscribers(authorId);
        } catch (FeignException e) {
            log.warn("Error during getting user's subscribers. Skip creating notification");
        }
        return userSubscribers;
    }
}
