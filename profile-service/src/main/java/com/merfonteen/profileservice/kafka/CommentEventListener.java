package com.merfonteen.profileservice.kafka;

import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
import com.merfonteen.profileservice.service.CommentProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentEventListener {
    private final CommentProjectionService commentProjectionService;

    @KafkaListener(topics = "${topic.comment-created}", groupId = "profile-group")
    public void handleCommentCreatedEvent(CommentCreatedEvent event, Acknowledgment ack) {
        log.info("Profile service received a comment-created-event '{}'", event);
        commentProjectionService.applyCommentCreated(event);
        ack.acknowledge();
    }

    @KafkaListener(topics = "${topic.comment-removed}", groupId = "profile-group")
    public void handleCommentRemovedEvent(CommentRemovedEvent event, Acknowledgment ack) {
        log.info("Profile service received a comment-removed-event '{}'", event);
        commentProjectionService.applyCommentRemoved(event);
        ack.acknowledge();
    }
}
