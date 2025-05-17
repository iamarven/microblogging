package com.merfonteen.likeservice.kafkaProducer;

import com.merfonteen.likeservice.dto.kafkaEvent.LikeRemovedEvent;
import com.merfonteen.likeservice.dto.kafkaEvent.LikeSentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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
    }

    public void sendLikeRemovedEvent(LikeRemovedEvent event) {
        kafkaTemplate.send(likeRemovedTopic, event.getLikeId().toString(), event);
    }
}
