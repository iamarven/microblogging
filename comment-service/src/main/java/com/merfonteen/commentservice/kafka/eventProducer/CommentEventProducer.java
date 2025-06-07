package com.merfonteen.commentservice.kafka.eventProducer;

import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentEventProducer {

    @Value("${topic.comment-created}")
    private String commentCreatedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendCommentCreatedEvent(CommentCreatedEvent event) {
        kafkaTemplate.send(commentCreatedTopic, event.getCommentId().toString(), event);
        log.info("New message was sent to topic comment-created-event successfully: {}", event);
    }
}
