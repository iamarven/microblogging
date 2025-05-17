package com.merfonteen.feedservice.kafkaListener;

import com.merfonteen.feedservice.dto.event.PostCreatedEvent;
import com.merfonteen.feedservice.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCreatedEventListener {

    @Value("${topic.post-created}")
    private String topic;
    private final FeedService feedService;

    @KafkaListener(topics = "post-created-event", groupId = "feed-group")
    public void handlePostCreated(PostCreatedEvent event, Acknowledgment ack) {
        log.info("Received event: {}", event);
        feedService.distributePostToSubscribers(event);
        ack.acknowledge();
    }
}
