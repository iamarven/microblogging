package com.merfonteen.postservice.kafkaProducer;

import com.merfonteen.postservice.dto.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PostEventProducer {

    private final KafkaTemplate<String, PostCreatedEvent> kafkaTemplate;

    public void sendPostCreatedEvent(PostCreatedEvent event) {
        kafkaTemplate.send("post-created", event.getPostId().toString(), event);
    }
}
