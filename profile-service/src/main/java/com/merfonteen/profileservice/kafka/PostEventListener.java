package com.merfonteen.profileservice.kafka;

import com.merfonteen.kafkaEvents.PostCreatedEvent;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import com.merfonteen.profileservice.service.CacheService;
import com.merfonteen.profileservice.service.PostProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static com.merfonteen.profileservice.config.RedisConfig.USER_POSTS_CACHE;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostEventListener {
    private final CacheService cacheService;
    private final PostProjectionService postProjectionService;

    @KafkaListener(
            topics = "${topic.post-created}",
            groupId = "profile-group",
            containerFactory = "postCreatedContainerFactory"
    )
    public void handlePostCreatedEvent(PostCreatedEvent event, Acknowledgment ack) {
        log.info("Profile service received a post-created-event '{}'", event);
        postProjectionService.applyPostCreated(event);
        ack.acknowledge();
        cacheService.invalidateCacheByEntityId(USER_POSTS_CACHE, event.getAuthorId());
    }

    @KafkaListener(
            topics = "${topic.post-removed}",
            groupId = "profile-group",
            containerFactory = "postRemovedContainerFactory"
    )
    public void handlePostRemovedEvent(PostRemovedEvent event, Acknowledgment ack) {
        log.info("Profile service received a post-removed-event '{}'", event);
        postProjectionService.applyPostRemoved(event);
        ack.acknowledge();
        cacheService.invalidateCacheByEntityId(USER_POSTS_CACHE, event.getAuthorId());
    }
}
