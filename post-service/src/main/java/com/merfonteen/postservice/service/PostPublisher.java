package com.merfonteen.postservice.service;

import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.postservice.kafkaProducer.PostEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostPublisher {
    private final PostEventProducer postEventProducer;

    public void publishPostCreatedEvent(Long postId, Long authorId) {
        log.debug("Publishing post created event for postId: {}, authorId: {}", postId, authorId);
        postEventProducer.sendPostCreatedEvent(new PostCreatedEvent(postId, authorId, Instant.now()));
    }

    public void publishPostRemovedEvent(Long postId, Long authorId) {
        log.debug("Publishing post removed event for postId: {}, authorId: {}", postId, authorId);
        postEventProducer.sendPostRemovedEvent(new PostRemovedEvent(postId, authorId));
    }
}
