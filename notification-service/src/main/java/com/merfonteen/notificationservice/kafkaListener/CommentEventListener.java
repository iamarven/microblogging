package com.merfonteen.notificationservice.kafkaListener;

import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${topic.comment-created}")
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        log.info("Received comment-created-event: {}", event);

    }
}
