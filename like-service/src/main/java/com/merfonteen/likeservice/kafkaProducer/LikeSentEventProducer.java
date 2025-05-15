package com.merfonteen.likeservice.kafkaProducer;

import com.merfonteen.likeservice.dto.kafkaEvent.LikeSentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LikeSentEventProducer {

    @Value("${topic.like-sent}")
    private String topic;

    private final KafkaTemplate<String, LikeSentEvent> kafkaTemplate;

    public void sendLikeSentEvent(LikeSentEvent event) {
        kafkaTemplate.send(topic, event.getLikeId().toString(), event);
    }
}
