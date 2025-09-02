package com.merfonteen.profileservice.kafka;

import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.profileservice.service.PostProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PostEventListener {
    private final PostProjectionService postProjectionService;

    @KafkaListener(topics = "${topic.post-created}", groupId = "profile-group")
    public void handlePostCreatedEvent(PostCreatedEvent postCreatedEvent, Acknowledgment ack) {
        postProjectionService.applyPostCreated(postCreatedEvent);
        ack.acknowledge();
    }

    @KafkaListener(topics = "${topic.post-removed}", groupId = "profile-group")
    public void handlePostRemovedEvent(PostRemovedEvent postRemovedEvent, Acknowledgment ack) {
        postProjectionService.applyPostRemoved(postRemovedEvent);
        ack.acknowledge();
    }
}
