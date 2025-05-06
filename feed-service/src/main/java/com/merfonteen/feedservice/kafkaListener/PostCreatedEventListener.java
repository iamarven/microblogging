package com.merfonteen.feedservice.kafkaListener;

import com.merfonteen.feedservice.dto.PostCreatedEvent;
import com.merfonteen.feedservice.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCreatedEventListener {

    private final FeedService feedService;

    @KafkaListener(topics = "post-created", groupId = "feed-group")
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("Received event: {}", event);
        feedService.distributePostToSubscribers(event);
    }
}
