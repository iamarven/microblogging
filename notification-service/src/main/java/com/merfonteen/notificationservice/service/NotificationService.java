package com.merfonteen.notificationservice.service;

import com.merfonteen.notificationservice.dto.NotificationResponse;
import com.merfonteen.notificationservice.dto.NotificationsPageResponse;
import com.merfonteen.notificationservice.dto.NotificationsSearchRequest;
import com.merfonteen.notificationservice.model.enums.NotificationType;

public interface NotificationService {

    NotificationsPageResponse getMyNotifications(Long currentUserId, NotificationsSearchRequest searchRequest);

    Long countUnreadNotifications(Long currentUserId);

    NotificationResponse markAsRead(Long notificationId, Long currentUserId);

    void deleteNotification(Long id, Long currentUserId);

    void deleteNotificationsForEntity(Long entityId, NotificationType type);

    void sendLikeNotification(Long senderId, Long likeId, Long postId);

    void sendPostNotification(Long postId, Long authorId);

    void sendFollowNotification(Long followerId, Long followeeId, Long subscriptionId);

    void sendCommentNotification(Long commentId, Long postId, Long leftCommentUserId);
}
