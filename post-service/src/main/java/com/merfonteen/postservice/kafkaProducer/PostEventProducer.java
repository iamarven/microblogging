package com.merfonteen.postservice.kafkaProducer;

import com.merfonteen.postservice.dto.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PostEventProducer {

    @Value("${topic.post-created}")
    private String topic;

    private final KafkaTemplate<String, PostCreatedEvent> kafkaTemplate;

    public void sendPostCreatedEvent(PostCreatedEvent event) {
        kafkaTemplate.send(topic, event.getPostId().toString(), event);
    }
}
