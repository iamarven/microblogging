package com.merfonteen.notificationservice.service;

public interface NotificationService {
    void sendLikeNotification(Long senderId, Long likeId, Long postId);
}
