package com.merfonteen.notificationservice.kafkaListener;

import com.merfonteen.kafkaEvents.LikeRemovedEvent;
import com.merfonteen.kafkaEvents.LikeSentEvent;
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
public class LikeEventListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${topic.like-sent}",
            groupId = "notification-group",
            containerFactory = "likeSentContainerFactory"
    )
    public void handleLikeSentEvent(LikeSentEvent event, Acknowledgment ack) {
        log.info("Received like-sent-event: {}", event);
        notificationService.sendLikeNotification(event.getUserId(), event.getLikeId(), event.getPostId());
        ack.acknowledge();
    }

    @KafkaListener(
            topics = "${topic.like-removed}",
            groupId = "notification-group",
            containerFactory = "likeRemovedContainerFactory"
    )
    public void handleLikeRemovedEvent(LikeRemovedEvent event, Acknowledgment ack) {
        log.info("Received like-removed-event: {}", event);
        notificationService.deleteNotificationsForEntity(event.getLikeId(), NotificationType.LIKE);
        ack.acknowledge();
    }
}
