package com.merfonteen.profileservice.kafka;

import com.merfonteen.kafkaEvents.CommentCreatedEvent;
import com.merfonteen.kafkaEvents.CommentRemovedEvent;
import com.merfonteen.profileservice.service.CommentProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CommentEventListener {
    private final CommentProjectionService commentProjectionService;

    @KafkaListener(topics = "${topic.comment-created}", groupId = "profile-group")
    public void handleCommentCreatedEvent(CommentCreatedEvent event, Acknowledgment ack) {
        commentProjectionService.applyCommentCreated(event);
        ack.acknowledge();
    }

    @KafkaListener(topics = "${topic.comment-removed}", groupId = "profile-group")
    public void handleCommentRemovedEvent(CommentRemovedEvent event, Acknowledgment ack) {
        commentProjectionService.applyCommentRemoved(event);
        ack.acknowledge();
    }
}
