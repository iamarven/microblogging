package com.merfonteen.notificationservice.service;

import com.merfonteen.notificationservice.dto.NotificationDto;
import com.merfonteen.notificationservice.dto.NotificationsPageDto;

public interface NotificationService {
    NotificationsPageDto getMyNotifications(Long currentUserId, int page, int size);
    Long countUnreadNotifications(Long currentUserId);
    NotificationDto markAsRead(Long notificationId, Long currentUserId);
    void sendLikeNotification(Long senderId, Long likeId, Long postId);
    void sendPostNotification(Long postId, Long authorId);
    void sendFollowNotification(Long followerId, Long followeeId, Long subscriptionId);
}
