package com.merfonteen.feedservice.kafka.eventListener;

import com.merfonteen.feedservice.service.FeedService;
import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {
    private final FeedService feedService;

    @KafkaListener(topics = "${topic.post-created}", groupId = "feed-group")
    public void handlePostCreated(PostCreatedEvent event, Acknowledgment ack) {
        log.info("Received post-created-event: {}", event);
        feedService.distributePostToSubscribers(event);
        ack.acknowledge();
    }

    @KafkaListener(topics = "${topic.post-removed}", groupId = "feed-group")
    public void handlePostRemoved(PostRemovedEvent event, Acknowledgment ack) {
        log.info("Received post-removed-event: {}", event);
        feedService.deleteFeedsByPostId(event);
        ack.acknowledge();
    }
}
