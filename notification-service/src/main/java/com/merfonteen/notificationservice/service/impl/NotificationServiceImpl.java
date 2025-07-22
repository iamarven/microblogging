package com.merfonteen.notificationservice.service.impl;

import com.merfonteen.exceptions.BadRequestException;
import com.merfonteen.exceptions.NotFoundException;
import com.merfonteen.notificationservice.client.FeedClient;
import com.merfonteen.notificationservice.client.PostClient;
import com.merfonteen.notificationservice.dto.NotificationDto;
import com.merfonteen.notificationservice.dto.NotificationsPageDto;
import com.merfonteen.notificationservice.dto.SubscriptionDto;
import com.merfonteen.notificationservice.mapper.NotificationMapper;
import com.merfonteen.notificationservice.model.Notification;
import com.merfonteen.notificationservice.model.enums.NotificationFilter;
import com.merfonteen.notificationservice.model.enums.NotificationType;
import com.merfonteen.notificationservice.repository.NotificationRepository;
import com.merfonteen.notificationservice.service.NotificationService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationServiceImpl implements NotificationService {

    private final PostClient postClient;
    private final FeedClient feedClient;
    private final NotificationMapper notificationMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationRepository notificationRepository;

    @Transactional
    @Override
    public NotificationsPageDto getMyNotifications(Long currentUserId, int page, int size, String filterRaw) {
        if (size > 100) {
            size = 100;
        }
        NotificationFilter filter = NotificationFilter.from(filterRaw);

        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> userNotifications;

        switch (filter) {
            case READ -> userNotifications =
                    notificationRepository.findAllByReceiverIdAndIsReadTrue(currentUserId, request);
            case UNREAD -> userNotifications =
                    notificationRepository.findAllByReceiverIdAndIsReadFalse(currentUserId, request);
            default -> userNotifications =
                    notificationRepository.findAllByReceiverId(currentUserId, request);
        }

        List<NotificationDto> notificationDtos = notificationMapper.toDtos(userNotifications.getContent());
        log.info("User '{}' fetched {} notifications (page={}, size={})",
                currentUserId, userNotifications.getNumberOfElements(), page, size);

        for (Notification notification : userNotifications) {
            notification.setIsRead(true);
        }

        notificationRepository.saveAll(userNotifications);
        stringRedisTemplate.opsForValue().set("user:notifications:unread:count:" + currentUserId, "0");

        return NotificationsPageDto.builder()
                .notifications(notificationDtos)
                .currentPage(userNotifications.getNumber())
                .totalPages(userNotifications.getTotalPages())
                .totalElements(userNotifications.getTotalElements())
                .isLastPage(userNotifications.isLast())
                .build();
    }

    @Override
    public Long countUnreadNotifications(Long currentUserId) {
        String cacheKey = "user:notifications:unread:count:" + currentUserId;
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);

        if (cacheValue != null) {
            return Long.parseLong(cacheValue);
        }

        Long countFromDb = notificationRepository.countAllByReceiverIdAndIsReadFalse(currentUserId);
        stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(countFromDb), Duration.ofMinutes(10));

        return countFromDb;
    }

    @Transactional
    @Override
    public NotificationDto markAsRead(Long notificationId, Long currentUserId) {
        Optional<Notification> notification = notificationRepository.findByIdAndReceiverId(notificationId, currentUserId);

        if(notification.isEmpty()) {
            log.warn("User '{}' tried to mark as read non-existing notification '{}'", currentUserId, notificationId);
            throw new NotFoundException(
                    String.format("Doesn't exist notification with id '%d' and user id '%d'",
                            notificationId, currentUserId));
        }

        notification.get().setIsRead(true);
        Notification saved = notificationRepository.save(notification.get());
        stringRedisTemplate.opsForValue().decrement("user:notifications:unread:count:" + currentUserId);

        return notificationMapper.toDto(saved);
    }

    @Transactional
    @Override
    public NotificationDto deleteNotification(Long id, Long currentUserId) {
        Optional<Notification> notificationToDelete = notificationRepository.findById(id);
        if(notificationToDelete.isPresent()) {
            if(!Objects.equals(notificationToDelete.get().getReceiverId(), currentUserId)) {
                throw new BadRequestException("You cannot delete not your own notifications");
            }
        } else {
            throw new NotFoundException(String.format("Notification with id '%d' not found", id));
        }
        notificationRepository.delete(notificationToDelete.get());
        log.info("Deleted a notification '{}' by user '{}'", id, currentUserId);
        return notificationMapper.toDto(notificationToDelete.get());
    }

    @Transactional
    @Override
    public void deleteNotificationsForEntity(Long entityId, NotificationType type) {
        log.info("Deleting all notifications for entity with id '{}' and type: {}", entityId, type);
        notificationRepository.deleteByEntityIdAndType(entityId, type);
    }

    @Transactional
    @Override
    public void sendLikeNotification(Long senderId, Long likeId, Long postId) {
        Long postAuthorId = postClient.getPostAuthorId(postId);

        Notification notification = Notification.builder()
                .senderId(senderId)
                .receiverId(postAuthorId)
                .entityId(postId)
                .type(NotificationType.LIKE)
                .message(String.format("User with id '%d' has liked your post with id '%d'", senderId, postId))
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        stringRedisTemplate.opsForValue().increment("user:notifications:unread:count:" + postAuthorId);
        notificationRepository.save(notification);
    }

    @Transactional
    @Override
    public void sendPostNotification(Long postId, Long authorId) {
        List<SubscriptionDto> userSubscribers = getUserSubscribers(authorId);
        List<Notification> buffer = new ArrayList<>();

        if (!userSubscribers.isEmpty()) {
            for (SubscriptionDto subscription : userSubscribers) {
                Notification notification = Notification.builder()
                        .senderId(authorId)
                        .receiverId(subscription.getFollowerId())
                        .entityId(postId)
                        .type(NotificationType.POST)
                        .message(String.format("User with id '%d' has published a new post with id '%d'", authorId, postId))
                        .isRead(false)
                        .createdAt(Instant.now())
                        .build();

                buffer.add(notification);
                stringRedisTemplate.opsForValue().increment("user:notifications:unread:count:" + subscription.getFollowerId());

                if (buffer.size() == 50) {
                    notificationRepository.saveAll(buffer);
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
        Notification notification = Notification.builder()
                .senderId(followerId)
                .receiverId(followeeId)
                .entityId(subscriptionId)
                .type(NotificationType.SUBSCRIPTION)
                .message(String.format("User with id '%d' has just subscribed to user with id '%d'", followerId, followeeId))
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        stringRedisTemplate.opsForValue().increment("user:notifications:unread:count:" + followeeId);
        notificationRepository.save(notification);
    }

    @Override
    public void sendCommentNotification(Long commentId, Long postId, Long leftCommentUserId) {
        Long postAuthorId = postClient.getPostAuthorId(postId);

        Notification notification = Notification.builder()
                .senderId(leftCommentUserId)
                .receiverId(postAuthorId)
                .entityId(commentId)
                .type(NotificationType.COMMENT)
                .message(String.format("User with id '%d' has just left comment '%d' to post with id '%d'",
                        leftCommentUserId, commentId, postId)
                )
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        stringRedisTemplate.opsForValue().increment("user:notifications:unread:count:" + postAuthorId);
        notificationRepository.save(notification);
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
        } catch (FeignException.NotFound e) {
            log.warn("Error during getting user's subscribers. Skip creating notification");
        }
        return userSubscribers;
    }
}
