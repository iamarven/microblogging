package com.merfonteen.likeservice.kafka.eventListener;

import com.merfonteen.likeservice.dto.kafkaEvent.PostRemovedEvent;
import com.merfonteen.likeservice.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostEventListener {

    private final LikeService likeService;

    @KafkaListener(topics = "${topic.post-removed}", groupId = "like-group")
    public void deleteLikesOnPost(PostRemovedEvent event, Acknowledgment ack) {
        log.info("Received post-removed-event: {}", event);
        likeService.removeLikesOnPost(event);
        ack.acknowledge();
    }
}
