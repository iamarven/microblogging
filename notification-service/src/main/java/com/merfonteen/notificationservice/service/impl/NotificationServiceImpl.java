package com.merfonteen.notificationservice.service.impl;

import com.merfonteen.notificationservice.repository.NotificationRepository;
import com.merfonteen.notificationservice.client.PostClient;
import com.merfonteen.notificationservice.exception.NotFoundException;
import com.merfonteen.notificationservice.model.Notification;
import com.merfonteen.notificationservice.model.enums.NotificationType;
import com.merfonteen.notificationservice.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationServiceImpl implements NotificationService {

    private final PostClient postClient;
    private final NotificationRepository notificationRepository;

    @Transactional
    @Override
    public void sendLikeNotification(Long senderId, Long likeId, Long postId) {
        Long postAuthorId;

        try {
            postAuthorId = postClient.getPostAuthorId(postId);
        } catch (NotFoundException e) {
            log.warn("Post with id '{}' not found. Skipping notification.", postId);
            return;
        }

        Notification notification = Notification.builder()
                .senderId(senderId)
                .receiverId(postAuthorId)
                .entityId(postId)
                .type(NotificationType.LIKE)
                .message(String.format("User with id '%d' has liked your post with id '%d'", senderId, postId))
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);
    }
}
