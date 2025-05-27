package com.merfonteen.notificationservice.kafkaListener;

import com.merfonteen.notificationservice.dto.event.PostCreatedEvent;
import com.merfonteen.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${topic.post-created}", groupId = "notification-group")
    public void handlePostCreatedEvent(PostCreatedEvent event, Acknowledgment ack) {
        log.info("Received post-created-event: {}", event);
        notificationService.sendPostNotification(event.getPostId(), event.getAuthorId());
        ack.acknowledge();
    }
}
