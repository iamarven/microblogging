package com.merfonteen.postservice.kafka;

import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostEventProducer {

    @Value("${topic.post-created}")
    private String postCreatedTopic;

    @Value("${topic.post-removed}")
    private String postRemovedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPostCreatedEvent(PostCreatedEvent event) {
        kafkaTemplate.send(postCreatedTopic, event.getPostId().toString(), event);
        log.info("PostCreatedEvent sent to topic '{}': {}", postCreatedTopic, event);
    }

    public void sendPostRemovedEvent(PostRemovedEvent event) {
        kafkaTemplate.send(postRemovedTopic, event.getPostId().toString(), event);
        log.info("PostRemovedEvent sent to topic '{}': {}", postRemovedTopic, event);
    }
}
