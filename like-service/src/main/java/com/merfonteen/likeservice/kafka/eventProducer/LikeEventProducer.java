package com.merfonteen.likeservice.kafka.eventProducer;

import com.merfonteen.kafkaEvents.LikeRemovedEvent;
import com.merfonteen.kafkaEvents.LikeSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class LikeEventProducer {

    @Value("${topic.like-sent}")
    private String likeSentTopic;

    @Value("${topic.like-removed}")
    private String likeRemovedTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendLikeSentEvent(LikeSentEvent event) {
        kafkaTemplate.send(likeSentTopic, event.getLikeId().toString(), event);
        log.info("New message was sent to topic 'like-sent' successfully: {}", event);
    }

    public void sendLikeRemovedEvent(LikeRemovedEvent event) {
        kafkaTemplate.send(likeRemovedTopic, event.getLikeId().toString(), event);
    }
}
