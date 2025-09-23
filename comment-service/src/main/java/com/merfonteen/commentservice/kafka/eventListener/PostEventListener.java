package com.merfonteen.commentservice.kafka.eventListener;

import com.merfonteen.commentservice.service.CommentService;
import com.merfonteen.kafkaEvents.PostRemovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostEventListener {

    private final CommentService commentService;

    @KafkaListener(
            topics = "${topic.post-removed}",
            groupId = "comment-group",
            containerFactory = "postRemovedContainerFactory"
    )
    public void handlePostRemovedEvent(PostRemovedEvent event, Acknowledgment ack) {
        log.info("Received post-removed-event: {}", event);
        commentService.removeCommentsOnPost(event);
        ack.acknowledge();
    }
}
