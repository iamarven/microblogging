package com.merfonteen.notificationservice.service;

import com.merfonteen.notificationservice.dto.NotificationDto;
import com.merfonteen.notificationservice.dto.NotificationsPageDto;
import com.merfonteen.notificationservice.model.enums.NotificationType;

public interface NotificationService {
    NotificationsPageDto getMyNotifications(Long currentUserId, int page, int size, String filter);
    Long countUnreadNotifications(Long currentUserId);
    NotificationDto markAsRead(Long notificationId, Long currentUserId);
    NotificationDto deleteNotification(Long id, Long currentUserId);
    void deleteNotificationsForEntity(Long entityId, NotificationType type);
    void sendLikeNotification(Long senderId, Long likeId, Long postId);
    void sendPostNotification(Long postId, Long authorId);
    void sendFollowNotification(Long followerId, Long followeeId, Long subscriptionId);
}
