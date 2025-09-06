package com.merfonteen.profileservice.kafka;

import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.profileservice.service.PostProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostEventListener {
    private final PostProjectionService postProjectionService;

    @KafkaListener(topics = "${topic.post-created}", groupId = "profile-group")
    public void handlePostCreatedEvent(PostCreatedEvent event, Acknowledgment ack) {
        log.info("Profile service received a post-created-event '{}'", event);
        postProjectionService.applyPostCreated(event);
        ack.acknowledge();
    }

    @KafkaListener(topics = "${topic.post-removed}", groupId = "profile-group")
    public void handlePostRemovedEvent(PostRemovedEvent event, Acknowledgment ack) {
        log.info("Profile service received a post-removed-event '{}'", event);
        postProjectionService.applyPostRemoved(event);
        ack.acknowledge();
    }
}
