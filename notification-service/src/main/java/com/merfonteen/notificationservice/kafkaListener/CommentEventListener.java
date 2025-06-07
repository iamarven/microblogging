package com.merfonteen.notificationservice.kafkaListener;

import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
import com.merfonteen.notificationservice.model.enums.NotificationType;
import com.merfonteen.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${topic.comment-created}", groupId = "notification-group")
    public void handleCommentCreatedEvent(CommentCreatedEvent event, Acknowledgment ack) {
        log.info("Received comment-created-event: {}", event);
        notificationService.sendCommentNotification(event.getCommentId(), event.getPostId(), event.getUserId());
        ack.acknowledge();
    }

    @KafkaListener(topics = "${topic.comment-removed}")
    public void handleCommentRemovedEvent(CommentRemovedEvent event, Acknowledgment ack) {
        log.info("Received comment-removed-event: {}", event);
        notificationService.deleteNotificationsForEntity(event.getCommentId(), NotificationType.COMMENT);
        ack.acknowledge();
    }
}
